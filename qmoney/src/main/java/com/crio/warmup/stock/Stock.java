package com.crio.warmup.stock;

public class Stock {

    public String symbol;
    public int quantity;
    public String tradeType;
    public String purchaseDate;
     
    public Stock(){

    }

    public Stock(String sym,int qu,String tt,String pd){
        symbol=sym;
        quantity=qu;
        tradeType=tt;
        purchaseDate=pd;
    }

}