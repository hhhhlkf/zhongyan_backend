package com.gosling.bms.response;

import com.gosling.bms.utils.enums.ResultCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;


@Data
public class ErrorResult implements Serializable {
    private Integer code;
    private String message;
    //private boolean success = false;
    //@JsonIgnore
    //private ResultCode resultCode;

    public static ErrorResult error() {
        ErrorResult result = new ErrorResult();
        result.setCode(ResultCode.INTERNAL_ERROR.code());
        result.setMessage(ResultCode.INTERNAL_ERROR.message());
        return result;
   }

    public static ErrorResult error(String message) {
        ErrorResult result = new ErrorResult();
        result.setCode(ResultCode.INTERNAL_ERROR.code());
        result.setMessage(message);
        return result;
   }

    public static ErrorResult error(Integer code, String message) {
        ErrorResult result = new ErrorResult();
        result.setCode(code);
        result.setMessage(message);
        return result;
   }

   public static ErrorResult error(ResultCode resultCode, String message) {
        ErrorResult result = new ErrorResult();
        result.setCode(resultCode.code());
        //result.setResultCode(resultCode);
        result.setMessage(message);
        return result;
   }
}
