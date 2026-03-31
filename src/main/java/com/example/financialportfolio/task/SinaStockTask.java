package com.example.financialportfolio.task;

import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;
import com.example.financialportfolio.service.StockService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class SinaStockTask {

    // ========== 核心依赖注入 ==========
    @Autowired
    private StockService stockService;

    @Autowired
    private RestTemplate restTemplate;

    // ========== 常量定义 ==========
    // 新浪实时行情API（正确地址，无多余斜杠）
    private static final String SINA_API_URL = "https://hq.sinajs.cn/list=";
    // 目标股票列表（sh/sz前缀必须保留，新浪API要求）
    private static final List<String> TARGET_CODES = List.of("sh600000", "sz000001", "sh600519");
    // A股实际交易时间常量（保留，可后续恢复过滤用）
    private static final LocalTime STOCK_OPEN_TIME = LocalTime.of(9, 30);
    private static final LocalTime STOCK_CLOSE_TIME = LocalTime.of(15, 0);

    // ========== 构造方法（验证Bean加载） ==========
    public SinaStockTask() {
        System.out.println("✅ SinaStockTask 定时任务Bean已加载！");
    }

    // ========== 手动测试方法（项目启动后立刻执行） ==========
    @PostConstruct
    public void manualFetchTest() {
        System.out.println("\n=============== 手动触发采集测试 ===============");
        // 强制遍历目标股票，直接采集
        for (String code : TARGET_CODES) {
            System.out.println("\n📌 开始采集：" + code);
            fetchAndSaveSingleStock(code);
        }
        System.out.println("=============== 手动采集测试结束 ===============\n");
    }

    // ========== 定时任务：每分钟触发一次（核心修改点） ==========
    /**
     * 每分钟整分触发一次（任意时间/任意星期，无时间过滤）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void fetchStockDataEvery5Minutes() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("\n⏰ 定时任务触发（每分钟）：" + now);

        // 【关键修改】注释时间过滤逻辑，确保每分钟都执行采集
        // LocalDate today = LocalDate.now();
        // LocalTime nowTime = LocalTime.now();
        // if (!isTradingDay(today) || nowTime.isBefore(STOCK_OPEN_TIME) || nowTime.isAfter(STOCK_CLOSE_TIME)) {
        //     System.out.println("⏳ 非交易时间（" + nowTime + "），跳过定时采集");
        //     return;
        // }

        // 遍历采集所有目标股票
        for (String code : TARGET_CODES) {
            fetchAndSaveSingleStock(code);
        }
    }

    // ========== 定时任务：15:00收盘采集（保留，可选） ==========
    @Scheduled(cron = "0 0 15 * * MON-FRI")
    public void fetchClosingData() {
        if (!isTradingDay(LocalDate.now())) {
            return;
        }

        for (String code : TARGET_CODES) {
            fetchAndSaveSingleStock(code);
        }
        System.out.println("📌 收盘数据定时采集完成");
    }

    // ========== 工具方法：判断是否为交易日 ==========
    private boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        // 仅过滤周末，法定节假日可后续扩展
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    // ========== 工具方法：字符串转BigDecimal（容错） ==========
    private BigDecimal parseStringToBigDecimal(String str) {
        if (str == null || str.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            System.err.println("⚠️ 数字格式错误：" + str + "，默认赋值0");
            return BigDecimal.ZERO;
        }
    }

    // ========== 工具方法：字符串转Long（容错） ==========
    private Long parseStringToLong(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            System.err.println("⚠️ 数字格式错误：" + str + "，默认赋值0");
            return 0L;
        }
    }

    // ========== 工具方法：时间对齐到最近5分钟整点 ==========
    private LocalDateTime alignToFiveMinutes(LocalDateTime time) {
        int minute = time.getMinute();
        int alignedMinute = (minute / 5) * 5;
        return time.withMinute(alignedMinute).withSecond(0).withNano(0);
    }

    // ========== 核心方法：采集单只股票数据并保存 ==========
    private void fetchAndSaveSingleStock(String stockCode) {
        // 校验股票代码格式
        if (stockCode == null || stockCode.length() < 2) {
            System.err.println("❌ 股票代码格式错误：" + stockCode);
            return;
        }

        // 拼接正确的API URL（无多余斜杠）
        String url = SINA_API_URL + stockCode;
        String response = null;

        try {
            // 1. 构造反爬请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 2. 调用新浪API
            ResponseEntity<String> entity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            response = entity.getBody();
            System.out.println("📡 API响应 [" + stockCode + "]: " + response);

            // 3. 校验响应有效性
            if (!StringUtils.hasText(response) || !response.contains("=\"")) {
                throw new IllegalArgumentException("返回数据无有效内容（空/格式错误）");
            }

            // 4. 解析API响应数据
            String dataStr = response.split("=\"")[1].replace("\";", "").trim();
            String[] fields = dataStr.split(",");
            if (fields.length < 10) {
                throw new IllegalArgumentException("数据字段不足（仅" + fields.length + "个），无法解析");
            }

            // 提取核心字段（新浪API固定索引）
            String stockName = fields[0]; // 股票名称
            BigDecimal openPrice = parseStringToBigDecimal(fields[1]); // 今开价
            BigDecimal currentPrice = parseStringToBigDecimal(fields[3]); // 最新价
            BigDecimal high = parseStringToBigDecimal(fields[4]); // 最高价
            BigDecimal low = parseStringToBigDecimal(fields[5]); // 最低价
            Long volumeHand = parseStringToLong(fields[8]); // 成交量（手）
            Long volume = volumeHand * 100; // 转换为股数（1手=100股）

            // 5. 封装数据对象
            String pureCode = stockCode.substring(2); // 去掉sh/sz前缀
            LocalDateTime ts = alignToFiveMinutes(LocalDateTime.now()); // 时间对齐到5分钟整点

            // 封装价格点数据
            StockPricePointResponse newPrice = new StockPricePointResponse();
            newPrice.setStockCode(pureCode);
            newPrice.setTs(ts);
            newPrice.setOpen(openPrice);
            newPrice.setHigh(high);
            newPrice.setLow(low);
            newPrice.setClose(currentPrice);
            newPrice.setVolume(volume);

            // 封装股票基础信息
            StockListItemResponse stockInfo = new StockListItemResponse();
            stockInfo.setCode(pureCode);
            stockInfo.setName(stockName);
            stockInfo.setMarket(stockCode.startsWith("sh") ? "上证" : "深证");
            stockInfo.setLatestPrice(currentPrice);

            // 6. 调用Service保存/更新数据
            stockService.updateStockInfo(stockInfo);
            stockService.saveStockPrice(newPrice);

            // 采集成功日志
            System.out.println("✅ 采集成功：" + stockName + "(" + pureCode + ") - 最新价：" + currentPrice + "，成交量：" + volume);

        } catch (IllegalArgumentException e) {
            // 解析异常（字段不足/格式错误）
            System.err.println("❌ 解析失败 [" + stockCode + "]: " + e.getMessage() + "，响应内容：" + response);
        } catch (RestClientException e) {
            // API请求异常（网络/403/超时）
            System.err.println("❌ API请求失败 [" + stockCode + "] URL：" + url + "，错误原因：" + e.getMessage());
        } catch (Exception e) {
            // 其他未知异常
            System.err.println("❌ 采集/保存失败 [" + stockCode + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}