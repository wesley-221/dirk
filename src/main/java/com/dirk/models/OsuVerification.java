/*
 * MIT License
 *
 * Copyright (c) 2020 Wesley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dirk.models;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
public class OsuVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String userSnowflake;
    private String serverSnowflake;

    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID userSecret;
    private Date expireDate;

    public OsuVerification() {
    }

    public static UUID getUserSecretFromURLSafeString(String urlSafeString) {
        byte[] bytes = Base64.decodeBase64(urlSafeString);
        ByteBuffer bb = ByteBuffer.wrap(bytes);

        return new UUID(bb.getLong(), bb.getLong());
    }

    public String getUserSecretAsURLSafeString() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

        bb.putLong(this.userSecret.getMostSignificantBits());
        bb.putLong(this.userSecret.getLeastSignificantBits());

        return Base64.encodeBase64URLSafeString(bb.array());
    }
}
