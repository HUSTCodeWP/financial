package com.example.financialportfolio.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.financialportfolio.common.exception.ResourceNotFoundException;
import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;
import com.example.financialportfolio.entity.Stock;
import com.example.financialportfolio.entity.StockPrice;
import com.example.financialportfolio.repository.StockPriceRepository;
import com.example.financialportfolio.repository.StockRepository;
import com.example.financialportfolio.service.StockService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final RestTemplate restTemplate;

    // 新浪K线API地址
    private static final String SINA_KLINE_URL =
            "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData";

    // A股交易时间常量
    private static final LocalTime STOCK_OPEN_TIME = LocalTime.of(9, 30);
    private static final LocalTime STOCK_CLOSE_TIME = LocalTime.of(15, 0);
    private static final LocalTime NOON_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime NOON_OPEN = LocalTime.of(13, 0);

    // 配置：回填服务启动前N天的历史数据（可根据需求调整）
    private static final int BACKFILL_DAYS = 7;

    // 法定节假日列表（示例，可扩展）
    private static final List<LocalDate> HOLIDAYS = Arrays.asList(
            LocalDate.of(2026, 1, 1),  // 元旦
            LocalDate.of(2026, 2, 10), // 春节
            LocalDate.of(2026, 4, 4),  // 清明
            LocalDate.of(2026, 5, 1),  // 劳动节
            LocalDate.of(2026, 6, 10), // 端午
            LocalDate.of(2026, 9, 17), // 中秋
            LocalDate.of(2026, 10, 1)  // 国庆
    );

    // 默认初始化的股票列表（解决启动时无激活股票问题）
    private static final List<StockListItemResponse> DEFAULT_STOCKS;

    // 静态代码块初始化（用setter赋值，适配无参构造的DTO）
    static {
        DEFAULT_STOCKS = new ArrayList<>();

        // 浦发银行
        StockListItemResponse stock1 = new StockListItemResponse();
        stock1.setCode("600000");
        stock1.setName("浦发银行");
        stock1.setMarket("SH");
        stock1.setLatestPrice(null);
        DEFAULT_STOCKS.add(stock1);

        // 平安银行
        StockListItemResponse stock2 = new StockListItemResponse();
        stock2.setCode("000001");
        stock2.setName("平安银行");
        stock2.setMarket("SZ");
        stock2.setLatestPrice(null);
        DEFAULT_STOCKS.add(stock2);

        // 贵州茅台
        StockListItemResponse stock3 = new StockListItemResponse();
        stock3.setCode("600519");
        stock3.setName("贵州茅台");
        stock3.setMarket("SH");
        stock3.setLatestPrice(null);
        DEFAULT_STOCKS.add(stock3);
    }

    public StockServiceImpl(StockRepository stockRepository,
                            StockPriceRepository stockPriceRepository,
                            RestTemplate restTemplate) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.restTemplate = restTemplate;
    }

    // ========== 新增：服务启动时主动回填历史数据（修复核心问题） ==========
    @PostConstruct
    public void initBackfillHistoryData() {
        System.out.println("\n===== 服务启动，开始回填历史数据 =====");

        // 第一步：自动初始化默认股票（解决启动时无激活股票问题）
        initDefaultStockData();

        // 获取所有激活的股票
        List<Stock> allStocks = stockRepository.findByIsActiveTrue();
        if (allStocks.isEmpty()) {
            System.out.println("无激活股票，跳过历史回填");
            return;
        }

        // 计算回填时间范围：服务启动前BACKFILL_DAYS天 到 启动前一刻
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(1); // 启动前1分钟
        LocalDateTime startTime = endTime.minusDays(BACKFILL_DAYS);

        // 逐个股票回填
        for (Stock stock : allStocks) {
            System.out.println("\n📌 开始回填股票：" + stock.getStockCode() + " - " + stock.getChineseName());
            backfillMissingPrices(stock.getStockCode(), startTime, endTime);
        }
        System.out.println("===== 历史数据回填完成 =====\n");
    }

    // ========== 新增：初始化默认股票数据 ==========
    private void initDefaultStockData() {
        System.out.println("📦 开始初始化默认股票数据...");
        for (StockListItemResponse defaultStock : DEFAULT_STOCKS) {
            // 仅当股票不存在时初始化
            if (stockRepository.findByStockCode(defaultStock.getCode()).isEmpty()) {
                updateStockInfo(defaultStock);
                System.out.println("✅ 初始化股票：" + defaultStock.getCode() + " - " + defaultStock.getName());
            } else {
                System.out.println("ℹ️ 股票已存在，跳过初始化：" + defaultStock.getCode());
            }
        }
    }

    @Override
    public List<StockListItemResponse> getAllStocks() {
        List<Stock> stocks = stockRepository.findByIsActiveTrueOrderByStockCodeAsc();
        List<StockListItemResponse> result = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        LocalDateTime startTime;
        LocalDateTime endTime;

        if (currentTime.isBefore(STOCK_OPEN_TIME)) {
            LocalDate previousTradingDay = getPreviousTradingDay(today);
            startTime = previousTradingDay.atTime(STOCK_OPEN_TIME);
            endTime = previousTradingDay.atTime(STOCK_CLOSE_TIME);
        } else if (currentTime.isAfter(STOCK_CLOSE_TIME)) {
            startTime = today.atTime(STOCK_OPEN_TIME);
            endTime = today.atTime(STOCK_CLOSE_TIME);
        } else {
            startTime = today.atTime(STOCK_OPEN_TIME);
            endTime = now;
        }

        for (Stock stock : stocks) {
            backfillMissingPrices(stock.getStockCode(), startTime, endTime);

            StockPrice latestPrice = stockPriceRepository.findTopByStockOrderByTsDesc(stock)
                    .orElse(null);

            StockListItemResponse response = new StockListItemResponse();
            response.setCode(stock.getStockCode());
            response.setName(stock.getChineseName());
            response.setMarket(stock.getMarket());
            response.setLatestPrice(latestPrice != null ? latestPrice.getClose() : null);
            result.add(response);
        }

        return result;
    }

    @Override
    public List<StockPricePointResponse> getStockPrices(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockCode));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        LocalDateTime startTime;
        LocalDateTime endTime;

        // 时间范围判断（修正：更精准的交易时段判断）
        if (currentTime.isBefore(STOCK_OPEN_TIME)) {
            // 开盘前：取上一交易日全天数据
            LocalDate previousTradingDay = getPreviousTradingDay(today);
            startTime = previousTradingDay.atTime(STOCK_OPEN_TIME);
            endTime = previousTradingDay.atTime(STOCK_CLOSE_TIME);
        } else if (currentTime.isAfter(STOCK_CLOSE_TIME)) {
            // 收盘后：取今日全天数据
            startTime = today.atTime(STOCK_OPEN_TIME);
            endTime = today.atTime(STOCK_CLOSE_TIME);
        } else {
            // 交易中：取今日开盘到当前时间
            startTime = today.atTime(STOCK_OPEN_TIME);
            endTime = now;
        }

        // 回填缺失数据
        backfillMissingPrices(stockCode, startTime, endTime);

        // 查询并返回数据
        List<StockPrice> prices = stockPriceRepository.findByStockAndTsBetweenOrderByTsAsc(
                stock, startTime, endTime
        );

        if (prices.isEmpty()) {
            throw new ResourceNotFoundException("No price data found for stock: " + stockCode);
        }

        return prices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ========== 优化：批量检查缺失点，提升效率 ==========
    @Override
    @Transactional // 批量操作加事务，保证原子性
    public void backfillMissingPrices(String stockCode, LocalDateTime startTime, LocalDateTime endTime) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + stockCode));

        // 1. 生成时间范围内所有应有的5分钟点（支持跨交易日）
        List<LocalDateTime> expectedPoints = generateFiveMinutePoints(startTime, endTime);
        if (expectedPoints.isEmpty()) {
            System.out.println("[" + stockCode + "] 无需要检查的5分钟点");
            return;
        }

        // 2. 批量查询已存在的点（替代逐个exists，提升效率）
        Set<LocalDateTime> existingPoints = stockPriceRepository.findExistingTsByStockAndTsIn(stock, expectedPoints)
                .stream().collect(Collectors.toSet());

        // 3. 计算缺失的点
        List<LocalDateTime> missingPoints = expectedPoints.stream()
                .filter(ts -> !existingPoints.contains(ts))
                .collect(Collectors.toList());

        System.out.println("===== 回填检查 [" + stockCode + "] =====");
        System.out.println("时间范围：" + startTime + " ~ " + endTime);
        System.out.println("应生成5分钟点数量：" + expectedPoints.size());
        System.out.println("已存在数量：" + existingPoints.size());
        System.out.println("缺失数量：" + missingPoints.size());

        if (missingPoints.isEmpty()) {
            System.out.println("[" + stockCode + "] 无缺失点，无需回填");
            return;
        }

        // 4. 拉取新浪历史数据（按缺失点的时间范围过滤）
        LocalDateTime minMissingTs = missingPoints.stream().min(LocalDateTime::compareTo).get();
        LocalDateTime maxMissingTs = missingPoints.stream().max(LocalDateTime::compareTo).get();
        List<StockPricePointResponse> historyBars = fetchHistoryBarsFromSina(stockCode, minMissingTs, maxMissingTs);

        // 5. 批量保存缺失数据
        int savedCount = 0;
        for (StockPricePointResponse bar : historyBars) {
            if (missingPoints.contains(bar.getTs())) {
                saveStockPrice(bar);
                savedCount++;
            }
        }

        System.out.println("[" + stockCode + "] 回填完成，新增数据：" + savedCount + "条");
        System.out.println("===== 回填检查结束 =====\n");
    }

    // ========== 优化：拉取新浪数据时增加时间过滤，避免无效数据 ==========
    private List<StockPricePointResponse> fetchHistoryBarsFromSina(
            String stockCode,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        String sinaSymbol = toSinaSymbol(stockCode);
        if (!StringUtils.hasText(sinaSymbol)) {
            System.err.println("[" + stockCode + "] 新浪Symbol转换失败");
            return new ArrayList<>();
        }

        // 新浪K线API参数（优化：datalen设为2000，覆盖更多历史点）
        String url = UriComponentsBuilder.fromUriString(SINA_KLINE_URL)
                .queryParam("symbol", sinaSymbol)
                .queryParam("scale", 5)       // 5分钟线
                .queryParam("ma", 0)          // 不返回均线
                .queryParam("datalen", 2000)  // 扩大返回数量，覆盖更多历史
                .toUriString();

        String response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> entity = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, String.class
            );
            response = entity.getBody();

            System.out.println("[" + stockCode + "] 新浪K线请求URL: " + url);
            System.out.println("[" + stockCode + "] 新浪K线原始返回长度: " + (response != null ? response.length() : 0));

        } catch (Exception e) {
            System.err.println("❌ [" + stockCode + "] 新浪K线请求失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }

        if (!StringUtils.hasText(response) || response.equals("[]")) {
            System.out.println("[" + stockCode + "] 新浪K线返回为空");
            return new ArrayList<>();
        }

        return parseSinaKlineResponseWithFastJson(stockCode, response, startTime, endTime);
    }

    // ========== 增强：解析时增加容错和日志 ==========
    private List<StockPricePointResponse> parseSinaKlineResponseWithFastJson(
            String stockCode,
            String response,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<StockPricePointResponse> result = new ArrayList<>();

        try {
            JSONArray klineArray = JSONArray.parseArray(response);
            for (int i = 0; i < klineArray.size(); i++) {
                JSONObject klineObj = klineArray.getJSONObject(i);

                // 提取字段并容错
                String dayStr = klineObj.getString("day");
                BigDecimal open = parseBigDecimal(klineObj.getString("open"));
                BigDecimal high = parseBigDecimal(klineObj.getString("high"));
                BigDecimal low = parseBigDecimal(klineObj.getString("low"));
                BigDecimal close = parseBigDecimal(klineObj.getString("close"));
                Long volume = parseLong(klineObj.getString("volume"));

                if (!StringUtils.hasText(dayStr)) {
                    System.out.println("跳过无效数据：day字段为空，索引=" + i);
                    continue;
                }

                // 解析时间（容错：处理格式异常）
                LocalDateTime ts;
                try {
                    ts = LocalDateTime.parse(dayStr.replace(" ", "T"));
                } catch (Exception e) {
                    System.err.println("[" + stockCode + "] 时间解析失败：" + dayStr + "，错误：" + e.getMessage());
                    continue;
                }

                // 严格过滤时间范围
                if (ts.isBefore(startTime) || ts.isAfter(endTime)) {
                    continue;
                }

                // 封装数据
                StockPricePointResponse bar = new StockPricePointResponse();
                bar.setStockCode(stockCode);
                bar.setTs(ts);
                bar.setOpen(open);
                bar.setHigh(high);
                bar.setLow(low);
                bar.setClose(close);
                bar.setVolume(volume);

                result.add(bar);
            }
        } catch (Exception e) {
            System.err.println("❌ [" + stockCode + "] 解析新浪K线数据失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[" + stockCode + "] 解析出有效历史数据：" + result.size() + "条");
        return result;
    }

    // ========== 优化：生成跨交易日的5分钟点 ==========
    private List<LocalDateTime> generateFiveMinutePoints(LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime cursor = alignToFiveMinutes(start);

        while (!cursor.isAfter(end)) {
            LocalDate cursorDate = cursor.toLocalDate();
            LocalTime cursorTime = cursor.toLocalTime();

            // 跳过非交易日（周末+法定节假日）
            if (!isTradingDay(cursorDate)) {
                cursor = cursor.plusDays(1).with(STOCK_OPEN_TIME); // 跳到下一个交易日开盘
                continue;
            }

            // 跳过午休时间（11:30-13:00）
            if (cursorTime.isAfter(NOON_CLOSE) && cursorTime.isBefore(NOON_OPEN)) {
                cursor = cursor.with(NOON_OPEN); // 直接跳到13:00
                continue;
            }

            // 只保留交易时间内的点
            if ((cursorTime.isAfter(STOCK_OPEN_TIME.minusMinutes(1)) && cursorTime.isBefore(NOON_CLOSE.plusMinutes(1)))
                    || (cursorTime.isAfter(NOON_OPEN.minusMinutes(1)) && cursorTime.isBefore(STOCK_CLOSE_TIME.plusMinutes(1)))) {
                result.add(cursor);
            }

            cursor = cursor.plusMinutes(5);
        }

        return result;
    }

    // ========== 完善：判断是否为交易日（周末+法定节假日） ==========
    private boolean isTradingDay(LocalDate date) {
        // 跳过周末
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        // 跳过法定节假日
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        return true;
    }

    // ========== 完善：获取上一交易日（支持法定节假日） ==========
    private LocalDate getPreviousTradingDay(LocalDate date) {
        LocalDate previous = date.minusDays(1);
        // 跳过周末+法定节假日
        while (!isTradingDay(previous)) {
            previous = previous.minusDays(1);
        }
        return previous;
    }

    // ========== 工具方法 ==========
    private String toSinaSymbol(String stockCode) {
        if (StringUtils.hasText(stockCode)) {
            if (stockCode.startsWith("sh") || stockCode.startsWith("sz")) {
                return stockCode;
            }
            // 沪市6开头，深市0/3开头
            if (stockCode.startsWith("6")) {
                return "sh" + stockCode;
            } else if (stockCode.startsWith("0") || stockCode.startsWith("3")) {
                return "sz" + stockCode;
            }
        }
        return null;
    }

    private LocalDateTime alignToFiveMinutes(LocalDateTime time) {
        int minute = time.getMinute();
        int alignedMinute = (minute / 5) * 5;
        return time.withMinute(alignedMinute).withSecond(0).withNano(0);
    }

    private BigDecimal parseBigDecimal(String str) {
        if (!StringUtils.hasText(str)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Long parseLong(String str) {
        if (!StringUtils.hasText(str)) {
            return 0L;
        }
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0L;
        }
    }

    private StockPricePointResponse toResponse(StockPrice price) {
        StockPricePointResponse resp = new StockPricePointResponse();
        resp.setStockCode(price.getStock().getStockCode());
        resp.setTs(price.getTs());
        resp.setOpen(price.getOpen());
        resp.setHigh(price.getHigh());
        resp.setLow(price.getLow());
        resp.setClose(price.getClose());
        resp.setVolume(price.getVolume());
        return resp;
    }

    @Override
    public void saveStockPrice(StockPricePointResponse response) {
        Stock stock = stockRepository.findByStockCode(response.getStockCode())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + response.getStockCode()));

        // 双重检查（避免并发问题）
        if (stockPriceRepository.existsByStockAndTs(stock, response.getTs())) {
            return;
        }

        StockPrice price = new StockPrice();
        price.setStock(stock);
        price.setTs(Objects.requireNonNullElse(response.getTs(), LocalDateTime.now()));
        price.setOpen(response.getOpen());
        price.setHigh(response.getHigh());
        price.setLow(response.getLow());
        price.setClose(response.getClose());
        price.setVolume(response.getVolume());

        stockPriceRepository.save(price);
    }

    @Override
    public void updateStockInfo(StockListItemResponse response) {
        Stock stock = stockRepository.findByStockCode(response.getCode())
                .orElse(new Stock());

        stock.setStockCode(response.getCode());
        stock.setChineseName(response.getName());
        stock.setMarket(response.getMarket());
        stock.setIsActive(true);
        // 补充创建/更新时间
        if (stock.getCreatedAt() == null) {
            stock.setCreatedAt(LocalDateTime.now());
        }
        stock.setUpdatedAt(LocalDateTime.now());

        stockRepository.save(stock);
    }
}