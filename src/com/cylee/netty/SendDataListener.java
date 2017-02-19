package com.cylee.netty;

/**
 * Created by cylee on 16/10/3.
 */

public interface SendDataListener {
    void onError(int errorCode);

    void onSuccess(String data);
}
