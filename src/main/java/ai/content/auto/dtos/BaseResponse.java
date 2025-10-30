package ai.content.auto.dtos;

import lombok.Getter;

@Getter
public class BaseResponse<T> {
    private String errorCode = "SUCCESS";
    private String errorMessage = "Operation completed successfully";
    private T data;

    public BaseResponse<T> setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public BaseResponse<T> setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public BaseResponse<T> setData(T data) {
        this.data = data;
        return this;
    }
}
