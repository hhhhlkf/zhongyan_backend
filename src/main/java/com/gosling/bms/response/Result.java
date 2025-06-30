package com.gosling.bms.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gosling.bms.utils.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    //private boolean success = true;
    private T data;
    @JsonIgnore
    //private ResultCode resultCode;

    private Result() {
   }

    public void setResultCode(ResultCode resultCode) {
        //this.resultCode = resultCode;
        this.code = resultCode.code();
        this.message = resultCode.message();
   }

    public Result(ResultCode resultCode, T data) {
        this.code = resultCode.code();
        this.message = resultCode.message();
        this.data = data;
   }

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.code());
        result.setMessage(ResultCode.SUCCESS.message());
        //result.setResultCode(ResultCode.SUCCESS);
        return result;
   }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        //result.setResultCode(ResultCode.SUCCESS);
        result.setCode(ResultCode.SUCCESS.code());
        result.setMessage(ResultCode.SUCCESS.message());
        result.setData(data);
        return result;
   }

    public static <T> Result<T> failure(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.code());
        result.setMessage(resultCode.message());
        return result;
    }
}

