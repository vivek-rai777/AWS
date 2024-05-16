package com.task10.pojo;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "cmtr-57544369-Tables-test")
public class Tables {
    @DynamoDBHashKey
    private int id;
    private int number;
    private int places;
    private boolean isVip;
    private int minOrder;

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public int getPlaces() {
        return places;
    }

    public boolean isVip() {
        return isVip;
    }

    public int getMinOrder() {
        return minOrder;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setPlaces(int places) {
        this.places = places;
    }

    public void setVip(boolean vip) {
        isVip = vip;
    }

    public void setMinOrder(int minOrder) {
        this.minOrder = minOrder;
    }
}
 