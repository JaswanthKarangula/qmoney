
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private StockQuotesService stockQuotesService;

  private RestTemplate restTemplate;

  @Deprecated
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {

    this.stockQuotesService = stockQuotesService;

  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  private static void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate.compareTo(endDate) >= 0) {
      throw new RuntimeException();
    }
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws StockQuoteServiceException {
    try {
      return stockQuotesService.getStockQuote(symbol, from, to);
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      throw new StockQuoteServiceException(e.getMessage(), e);

    }
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws StockQuoteServiceException {
    // TODO Auto-generated method stub

    List<AnnualizedReturn> result = new ArrayList<>();
    for (PortfolioTrade trade : portfolioTrades) {

      Candle startQuote = getStartStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      Candle endQuote = getEndStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      // System.out.println("Start Quote" +startQuote);
      // System.out.println("End Quote" +endQuote);
      AnnualizedReturn retu = calculateAnnualizedReturns(endDate, trade, startQuote, endQuote);
      result.add(retu);

    }
    Collections.sort(result, AnnualizedReturn.annualizedReturnComparator);
    return result;
  }

  private Candle getEndStockQuote(String symbol, LocalDate purchaseDate, LocalDate endDate)
      throws StockQuoteServiceException {
    List<Candle> stockQuoteList = getStockQuote(symbol, purchaseDate, endDate);
    // System.out.print(stockQuoteList);
    if (stockQuoteList.size() > 0) {
      return stockQuoteList.get(stockQuoteList.size() - 1);
    }
    return null;
  }

  private Candle getStartStockQuote(String symbol, LocalDate purchaseDate, LocalDate endDate)
      throws StockQuoteServiceException {
    List<Candle> stockQuoteList = getStockQuote(symbol, purchaseDate, endDate);
    /// System.out.print(stockQuoteList);
    if (stockQuoteList.size() > 0) {
      return stockQuoteList.get(0);
    }
    return null;
  }

  public static Double caluclateNumberOfYears(LocalDate start, LocalDate end) {
    double year = start.until(end, ChronoUnit.DAYS) / 365.24;
    return year;
  }

  public AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Candle startQuote,
      Candle endQuote) {
    try {
      Double buyPrice = startQuote.getOpen();
      Double sellPrice = endQuote.getClose();
      Double totalReturn = (sellPrice - buyPrice) / buyPrice;
      Double totalYears = caluclateNumberOfYears(trade.getPurchaseDate(), endDate);
      Double annualizedReturn = Math.pow((1 + totalReturn), 1 / totalYears) - 1;

      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
    } catch (Exception e) {
      return new AnnualizedReturn(trade.getSymbol(), Double.NaN, Double.NaN);
    }

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws StockQuoteServiceException {
    
    ExecutorService eService = Executors.newFixedThreadPool(numThreads);
    List<Future<AnnualizedReturn>> futures = new ArrayList<Future<AnnualizedReturn>>();
    for (PortfolioTrade trade : portfolioTrades) {
      Callable<AnnualizedReturn> callable = () -> {
        AnnualizedReturn annualizedReturn;
        List<Candle> q = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
        Candle startQuote = getStartStockQuote(q);
        Candle endQuote = getEndStockQuote(q);
        // System.out.println("StartQuote:   " + startQuote);
        // System.out.println("EndQuote:   " + endQuote);
        annualizedReturn = calculateAnnualizedReturns(endDate, trade, startQuote, endQuote);
        // System.out.println("AnnualiZedResut  :   " + annualizedReturn);
        // System.out.println("----------------------------------------------------------------------");
        return annualizedReturn;
      };

      Future<AnnualizedReturn> future = eService.submit(callable);
      futures.add(future);

      
    }

    List<AnnualizedReturn> result = new ArrayList<>();
    for (Future<AnnualizedReturn> f : futures) {
      
        try {
          result.add(f.get());
          
        } catch (Exception e) {
          throw new StockQuoteServiceException(e.getMessage(),e);
        }
      
    }
    eService.shutdown();
    Collections.sort(result,AnnualizedReturn.annualizedReturnComparator);
    return result;
  }

  private Candle getStartStockQuote(List<Candle> stockQuoteList) {
    if (stockQuoteList.size() > 0) {
      return stockQuoteList.get(0);
    }
    return null;
  }

  private Candle getEndStockQuote(List<Candle> stockQuoteList) {
    if (stockQuoteList.size() > 0) {
      return stockQuoteList.get(stockQuoteList.size() - 1);
    }
    return null;
  }

  
}


