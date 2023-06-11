
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  

  private static void validateDates(LocalDate startDate, LocalDate endDate) {
    if(startDate.compareTo(endDate)>=0){
      throw new RuntimeException();
    }
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException {
        try{
        validateDates(from,to);
        String uri=buildUri(symbol, from, to);
        // TiingoCandle c[]=new TiingoCandle[3];
        String json=restTemplate.getForObject(uri, String.class);
       // System.out.println(json);
        ObjectMapper objectMapper=getObjectMapper();
        List<TiingoCandle> l=objectMapper.readValue(json, new TypeReference<List<TiingoCandle>>() {
        });
        if(l==null){
          throw new Exception(json);
        }
        List<Candle> result=new ArrayList<>();
        for(TiingoCandle c:l){
         // Candle temp=c;
          result.add(c);
        }
        return result;
      }
      catch(JsonProcessingException e){
        throw new StockQuoteServiceException(e.getMessage(),e);
      }
      catch(Exception e){
        throw new StockQuoteServiceException(e.getMessage(),e);
      }
        
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String startDateString=startDate.toString();
    String endDateString=endDate.toString();
       String uriTemplate = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?"
            + "startDate="+startDateString+"&endDate="+endDateString+"&token=60f8347fa6d6c17fde693e38f9869a36de6a3035";
       return uriTemplate;
  }


  
}
