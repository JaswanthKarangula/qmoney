
package com.crio.warmup.stock.dto;

import java.util.Comparator;

public class AnnualizedReturn {

  private final String symbol;
  private final Double annualizedReturn;
  private final Double totalReturns;

  public AnnualizedReturn(String symbol, Double annualizedReturn, Double totalReturns) {
    this.symbol = symbol;
    this.annualizedReturn = annualizedReturn;
    this.totalReturns = totalReturns;
  }

  public String getSymbol() {
    return symbol;
  }

  public Double getAnnualizedReturn() {
    return annualizedReturn;
  }

  public Double getTotalReturns() {
    return totalReturns;
  }

  public static final Comparator<AnnualizedReturn> annualizedReturnComparator= new Comparator<AnnualizedReturn>(){

    public int compare(AnnualizedReturn a,AnnualizedReturn b){
      return (int)(b.getAnnualizedReturn().compareTo(a.getAnnualizedReturn()));
    }
  };

  @Override
  public String toString() {
    return "Annualised Return {"
            + "symbol=" + symbol
            + ", annualised return =" + annualizedReturn
            + ", totalreturn =" + totalReturns
            + '}';
  }
}
