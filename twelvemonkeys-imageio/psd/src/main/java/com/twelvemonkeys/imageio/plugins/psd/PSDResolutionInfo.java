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

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;
import java.io.IOException;

/**
 * PSDResolutionInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDResolutionInfo.java,v 1.0 May 2, 2008 3:58:19 PM haraldk Exp$
 */
class PSDResolutionInfo extends PSDImageResource {
    //    typedef struct _ResolutionInfo
    //    {
    //       LONG hRes;              /* Fixed-point number: pixels per inch */
    //       WORD hResUnit;          /* 1=pixels per inch, 2=pixels per centimeter */
    //       WORD WidthUnit;         /* 1=in, 2=cm, 3=pt, 4=picas, 5=columns */
    //       LONG vRes;              /* Fixed-point number: pixels per inch */
    //       WORD vResUnit;          /* 1=pixels per inch, 2=pixels per centimeter */
    //       WORD HeightUnit;        /* 1=in, 2=cm, 3=pt, 4=picas, 5=columns */
    //    } RESOLUTIONINFO;

    float mHRes;
    short mHResUnit;
    short mWidthUnit;
    float mVRes;
    short mVResUnit;
    short mHeightUnit;

    PSDResolutionInfo(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(ImageInputStream pInput) throws IOException {
        if (mSize != 16) {
            throw new IIOException("Resolution info length expected to be 16: " + mSize);
        }

        mHRes = PSDUtil.fixedPointToFloat(pInput.readInt());
        mHResUnit = pInput.readShort();
        mWidthUnit = pInput.readShort();
        mVRes = PSDUtil.fixedPointToFloat(pInput.readInt());
        mVResUnit = pInput.readShort();
        mHeightUnit = pInput.readShort();
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", hRes: ").append(mHRes);
        builder.append(" ");
        builder.append(resUnit(mHResUnit));
        builder.append(", width unit: ");
        builder.append(dimUnit(mWidthUnit));
        builder.append(", vRes: ").append(mVRes);
        builder.append(" ");
        builder.append(resUnit(mVResUnit));
        builder.append(", height unit: ");
        builder.append(dimUnit(mHeightUnit));

        builder.append("]");

        return builder.toString();
    }

    private String resUnit(final short pResUnit) {
        switch (pResUnit) {
            case 1:
                return "pixels/inch";
            case 2:
                return "pixels/cm";
            default:
                return "unknown unit " + pResUnit;
        }
    }

    private String dimUnit(final short pUnit) {
        switch (pUnit) {
            case 1:
                return "in";
            case 2:
                return "cm";
            case 3:
                return "pt";
            case 4:
                return "pica";
            case 5:
                return "column";
            default:
                return "unknown unit " + pUnit;
        }
    }
}
