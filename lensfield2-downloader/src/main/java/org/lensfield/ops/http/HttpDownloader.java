/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.ops.http;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.lensfield.api.LensfieldInput;
import org.lensfield.api.LensfieldOutput;
import org.lensfield.api.io.StreamIn;
import org.lensfield.api.io.StreamOut;

import java.io.IOException;

/**
 * @author sea36
 */
public class HttpDownloader {

    @LensfieldInput
    protected StreamIn in;

    @LensfieldOutput
    protected StreamOut out;

    public void run() throws Exception {
        String url = IOUtils.toString(in);
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        try {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                response.getEntity().writeTo(out);
            } else {
                throw new IOException("Unsupported HTTP Status: "+response.getStatusLine());
            }
        } finally {
            response.getEntity().consumeContent();
        }
    }


}
