package ru.melodin.fast.api;

import androidx.annotation.Nullable;

import java.io.IOException;

public class VKException extends IOException {

    private int code;

    private String url;
    private String message;
    private String captchaSid;
    private String captchaImg;
    private String redirectUri;

    public VKException(String url, String message, int code) {
        super(message);
        this.url = url;
        this.message = message;
        this.code = code;
    }

    @Override
    public String toString() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public String getCaptchaSid() {
        return captchaSid;
    }

    public String getCaptchaImg() {
        return captchaImg;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setCaptchaSid(String captchaSid) {
        this.captchaSid = captchaSid;
    }

    public void setCaptchaImg(String captchaImg) {
        this.captchaImg = captchaImg;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
