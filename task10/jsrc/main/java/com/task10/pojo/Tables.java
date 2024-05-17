package com.task10.pojo;
import com.google.gson.annotations.SerializedName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class Tables {
    private int id;
    private int number;
    private int places;
    @SerializedName("vip")
    private boolean isVip;
    private int minOrder;

    @DynamoDbPartitionKey
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
 