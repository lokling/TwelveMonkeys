/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.net.NetUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WritableCachedResponseImpl
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/WritableCachedResponseImpl.java#3 $
 */
class WritableCachedResponseImpl implements WritableCachedResponse {
    private final CachedResponseImpl mCachedResponse;

    /**
     * Creates a {@code WritableCachedResponseImpl}.
     */
    protected WritableCachedResponseImpl() {
        mCachedResponse = new CachedResponseImpl();
        // Hmmm..
        setHeader(HTTPCache.HEADER_CACHED_TIME, NetUtil.formatHTTPDate(System.currentTimeMillis()));
    }

    public CachedResponse getCachedResponse() {
        return mCachedResponse;
    }

    public void setHeader(String pName, String pValue) {
        setHeader(pName, pValue, false);
    }

    public void addHeader(String pName, String pValue) {
        setHeader(pName, pValue, true);
    }

    public Map<String, List<String>> getHeaders() {
        return mCachedResponse.mHeaders;
    }

    /**
     *
     * @param pName the header name
     * @param pValue the new header value
     * @param pAdd {@code true} if the value should add to the list of values, not replace existing value
     */
    private void setHeader(String pName, String pValue, boolean pAdd) {
        // System.out.println(" ++ CachedResponse ++ " + (pAdd ? "addHeader(" : "setHeader(") + pName + ", " + pValue + ")");
        // If adding, get list and append, otherwise replace list
        List<String> values = null;
        if (pAdd) {
            values = mCachedResponse.mHeaders.get(pName);
        }

        if (values == null) {
            values = new ArrayList<String>();

            if (pAdd) {
                // Add length of pName
                mCachedResponse.mHeadersSize += (pName != null ? pName.length() : 0);
            }
            else {
                // Remove length of potential replaced old values + pName
                String[] oldValues = getHeaderValues(pName);
                if (oldValues != null) {
                    for (String oldValue : oldValues) {
                        mCachedResponse.mHeadersSize -= oldValue.length();
                    }
                }
                else {
                    mCachedResponse.mHeadersSize += (pName != null ? pName.length() : 0);
                }
            }
        }

        // Add value, if not null
        if (pValue != null) {
            values.add(pValue);

            // Add length of pValue
            mCachedResponse.mHeadersSize += pValue.length();
        }

        // Always add to headers
        mCachedResponse.mHeaders.put(pName, values);
    }

    public OutputStream getOutputStream() {
        // TODO: Hmm.. Smells like DCL..?
        if (mCachedResponse.mContent == null) {
            createOutputStream();
        }
        return mCachedResponse.mContent;
    }

    public void setStatus(int pStatusCode) {
        mCachedResponse.mStatus = pStatusCode;
    }

    public int getStatus() {
        return mCachedResponse.getStatus();
    }

    private synchronized void createOutputStream() {
        ByteArrayOutputStream cache = mCachedResponse.mContent;
        if (cache == null) {
            String contentLengthStr = getHeaderValue("Content-Length");
            if (contentLengthStr != null) {
                int contentLength = Integer.parseInt(contentLengthStr);
                cache = new FastByteArrayOutputStream(contentLength);
            }
            else {
                cache = new FastByteArrayOutputStream(1024);
            }
            mCachedResponse.mContent = cache;
        }
    }

    public void writeHeadersTo(CacheResponse pResponse) {
        mCachedResponse.writeHeadersTo(pResponse);
    }

    public void writeContentsTo(OutputStream pStream) throws IOException {
        mCachedResponse.writeContentsTo(pStream);
    }

    public String[] getHeaderNames() {
        return mCachedResponse.getHeaderNames();
    }

    public String[] getHeaderValues(String pHeaderName) {
        return mCachedResponse.getHeaderValues(pHeaderName);
    }

    public String getHeaderValue(String pHeaderName) {
        return mCachedResponse.getHeaderValue(pHeaderName);
    }

    public int size() {
        return mCachedResponse.size();
    }

    public boolean equals(Object pOther) {
        if (pOther instanceof WritableCachedResponse) {
            // Take advantage of faster implementation
            return mCachedResponse.equals(((WritableCachedResponse) pOther).getCachedResponse());
        }
        return mCachedResponse.equals(pOther);
    }

    public int hashCode() {
        return mCachedResponse.hashCode();
    }
}