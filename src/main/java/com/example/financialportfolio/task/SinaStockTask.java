package com.example.financialportfolio.task;

import com.example.financialportfolio.dto.StockListItemResponse;
import com.example.financialportfolio.dto.StockPricePointResponse;
import com.example.financialportfolio.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SinaStockTask {

    @Autowired
    private StockService stockService;

    @Autowired
    private RestTemplate restTemplate;

    // 新浪财经API地址
    private static final String SINA_API_URL = "https://hq.sinajs.cn/list=";
    // 要采集的股票代码列表
    private static final List<String> TARGET_CODES = List.of("sh600000", "sz000001", "sh600519");

    // 限流控制：5分钟最多100次请求
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long lastResetTime = System.currentTimeMillis();
    private static final int MAX_REQUESTS = 100;
    private static final long TIME_WINDOW = 5 * 60 * 1000; // 300000毫秒 = 5分钟

    /**
     * 定时任务：每3秒执行一次采集
     */
    @Scheduled(fixedRate = 3000)
    public void fetchStockData() {
        long currentTime = System.currentTimeMillis();

        // 重置请求计数器（5分钟周期）
        if (currentTime - lastResetTime > TIME_WINDOW) {
            requestCount.set(0);
            lastResetTime = currentTime;
        }

        // 达到请求上限则停止本次采集
        if (requestCount.get() >= MAX_REQUESTS) {
            System.out.println("⚠️ 5分钟内请求次数已达上限（100次），本次跳过采集");
            return;
        }

        // 遍历采集每只股票
        for (String code : TARGET_CODES) {
            try {
                fetchAndSaveSingleStock(code);
                requestCount.incrementAndGet();
            } catch (Exception e) {
                System.err.println("❌ 采集股票失败 [" + code + "]: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 安全转换字符串为BigDecimal（处理空值/格式错误）
     */
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

    /**
     * 安全转换字符串为Long（处理空值/格式错误）
     */
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

    /**
     * 采集单只股票数据并保存到数据库
     */
    private void fetchAndSaveSingleStock(String stockCode) {
        // 防御性检查：避免股票代码格式错误导致substring越界
        if (stockCode == null || stockCode.length() < 2) {
            System.err.println("❌ 股票代码格式错误：" + stockCode);
            return;
        }

        // 构建API请求URL（兼容低版本Spring的fromUriString）
        String url = UriComponentsBuilder.fromUriString(SINA_API_URL)
                .path(stockCode)
                .toUriString();

        // 调用新浪API获取数据
        String response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            headers.set("User-Agent", "Mozilla/5.0");

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> entity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            response = entity.getBody();
        } catch (Exception e) {
            System.err.println("❌ API请求失败 [" + stockCode + "]: " + e.getMessage());
            return;
        }

        // 校验API返回数据有效性
        if (!StringUtils.hasText(response) || !response.contains("=\"")) {
            System.err.println("❌ API返回数据异常 [" + stockCode + "]: " + response);
            return;
        }

        // 解析新浪API返回的字符串数据
        String dataStr = response.split("=\"")[1].replace("\";", "").trim();
        String[] fields = dataStr.split(",");

        // 校验字段数量是否足够（至少9个字段）
        if (fields.length < 9) {
            System.err.println("❌ 股票[" + stockCode + "]数据字段不足（仅" + fields.length + "个），跳过");
            return;
        }

        // 提取并转换字段（核心修复：String → BigDecimal/Long）
        String stockName = StringUtils.hasText(fields[0]) ? fields[0] : "未知名称";
        BigDecimal openPrice = parseStringToBigDecimal(fields[1]);      // 开盘价
        BigDecimal currentPrice = parseStringToBigDecimal(fields[3]);   // 当前价/收盘价
        BigDecimal high = parseStringToBigDecimal(fields[4]);           // 最高价
        BigDecimal low = parseStringToBigDecimal(fields[5]);            // 最低价
        Long volume = parseStringToLong(fields[8]);                     // 成交量

        // 提取纯股票代码（去掉sh/sz前缀）
        String pureCode = stockCode.substring(2);

        // 封装行情数据（类型完全匹配，无标红）
        StockPricePointResponse newPrice = new StockPricePointResponse();
        newPrice.setStockCode(pureCode);
        newPrice.setTs(LocalDateTime.now());  // 时间戳（LocalDateTime类型）
        newPrice.setOpen(openPrice);          // BigDecimal类型
        newPrice.setHigh(high);               // BigDecimal类型
        newPrice.setLow(low);                 // BigDecimal类型
        newPrice.setClose(currentPrice);      // BigDecimal类型
        newPrice.setVolume(volume);           // Long类型

        // 封装股票基础信息
        StockListItemResponse stockInfo = new StockListItemResponse();
        stockInfo.setCode(pureCode);
        stockInfo.setName(stockName);
        stockInfo.setMarket(stockCode.startsWith("sh") ? "上证" : "深证");

        // 如果有 latestPrice
        try {
            stockInfo.setLatestPrice(currentPrice);
        } catch (Exception e) {
            // 没有这个字段就忽略
        }

        // 先存股票主数据
        stockService.updateStockInfo(stockInfo);

        // 再存价格
        stockService.saveStockPrice(newPrice);

        // 打印采集成功日志
        System.out.println("✅ 采集成功：" + stockName + "(" + pureCode + ") - 最新价：" + currentPrice);
    }
}