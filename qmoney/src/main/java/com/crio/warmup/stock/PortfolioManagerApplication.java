
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  static PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(new RestTemplate());

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    ObjectMapper objectMapper = getObjectMapper();
    File f = resolveFileFromResources(args[0]);
    List<PortfolioTrade> listStock = objectMapper.readValue(f, new TypeReference<List<PortfolioTrade>>() {
    });
    List<String> result = new ArrayList<String>();
    for (PortfolioTrade s : listStock) {
      result.add(s.getSymbol());
    }

    return result;
  }

  public static List<PortfolioTrade> getTradesList(String[] args) throws IOException, URISyntaxException {

    ObjectMapper objectMapper = getObjectMapper();
    File f = resolveFileFromResources(args[0]);
    List<PortfolioTrade> listStock = objectMapper.readValue(f, new TypeReference<List<PortfolioTrade>>() {
    });

    return listStock;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/jaswanthkarangula15-ME_QMONEY/qmoney/bin/test/assessments/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5b8dfcc1";
    String functionNameFromTestFileInStackTrace = "ModuleOneTest.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "19";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }


  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    
    String endDate=args[1];
    List<PortfolioTrade> trades=getTradesList(args);
    Map<Double,String> tmap=new TreeMap<>();
    for(PortfolioTrade trade:trades){
      String symbol=trade.getSymbol();
      String startDate=trade.getPurchaseDate().toString();
      List<TiingoCandle> c=getStockQuotes(symbol, LocalDate.parse(startDate), LocalDate.parse(endDate));
      try{
        if(c.size()==0){
          tmap.put((double) 0, symbol);
        }
        else{
          tmap.put(c.get(c.size()-1).getClose(), symbol);
        }
      }
      catch(Exception e){
        throw new RuntimeException();
      }
    }
    List<String> result= new ArrayList<String>();
      for( Map.Entry<Double,String> entry:tmap.entrySet()){
        result.add(entry.getValue());
        
      }
    return result;
 }



 public static List<TiingoCandle> getStockQuotes(String symbol,LocalDate from,LocalDate to){
  validateDates(from,to);
  String startDate=from.toString();
  String endDate=to.toString();
  String url="https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate+"&endDate="+endDate+"&token=60f8347fa6d6c17fde693e38f9869a36de6a3035";
  //System.out.println(url);
  RestTemplate restTemplate=new RestTemplate();
  TiingoCandle c[]=restTemplate.getForObject(url, TiingoCandle[].class);

  return Arrays.asList(c);
}

private static void validateDates(LocalDate startDate, LocalDate endDate) {
  if(startDate.compareTo(endDate)>=0){
    throw new RuntimeException();

  }
}

public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
        LocalDate endDate=LocalDate.parse(args[1]);
        List<PortfolioTrade> trades=getTradesList(args);
        List<AnnualizedReturn> result=new ArrayList<>();
        for(PortfolioTrade trade:trades){

          TiingoCandle startQuote=getStartStockQuotes(trade.getSymbol(), trade.getPurchaseDate());
          TiingoCandle endQuote=getEndStockQuotes(trade.getSymbol(), trade.getPurchaseDate(), endDate);
          AnnualizedReturn retu=calculateAnnualizedReturns(endDate, trade, startQuote.getOpen(), endQuote.getClose());
          result.add(retu);

        }
        Collections.sort(result,AnnualizedReturn.annualizedReturnComparator);


     return result;
  }

  public static TiingoCandle getEndStockQuotes(String symbol,LocalDate from,LocalDate to){
    validateDates(from,to);
    String startDate=from.toString();
    String endDate=to.toString();
    String url="https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate+"&endDate="+endDate+"&token=60f8347fa6d6c17fde693e38f9869a36de6a3035";
    //System.out.println(url);
    RestTemplate restTemplate=new RestTemplate();
    TiingoCandle c[]=restTemplate.getForObject(url, TiingoCandle[].class);

    if(c.length>0){
      return c[c.length-1];
    }
    return new TiingoCandle();
  }




  public static TiingoCandle getStartStockQuotes(String symbol,LocalDate on){
   
    String startDate=on.toString();
    String endDate=on.toString();
    String url="https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate+"&endDate="+endDate+"&token=60f8347fa6d6c17fde693e38f9869a36de6a3035";
    //System.out.println(url);
    RestTemplate restTemplate=new RestTemplate();
    TiingoCandle c[]=restTemplate.getForObject(url, TiingoCandle[].class);

    if(c.length>0){
      return c[c.length-1];
    }
    return new TiingoCandle();
  }

  public static Double caluclateNumberOfYears(LocalDate start,LocalDate end){
    double year = start.until(end, ChronoUnit.DAYS)/365.24;
    return year;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        Double totalReturn = (sellPrice - buyPrice) / buyPrice;
        Double totalYears=caluclateNumberOfYears(trade.getPurchaseDate(), endDate);
        Double annualizedReturn=Math.pow((1+totalReturn), 1/totalYears)-1;
        return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);

  }




  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using
  // PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and
  // endDate, and
  // call the newly implemented method in PortfolioManager to calculate the
  // annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns
  // as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();

    PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }


  private static String readFileAsString(String filename) throws URISyntaxException, IOException { 
  
    return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()), "UTF-8"); 
    }
   
   




















  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}

