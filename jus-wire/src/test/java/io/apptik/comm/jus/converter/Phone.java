// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: test.proto at 2:1
package io.apptik.comm.jus.converter;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class Phone extends Message<Phone, Phone.Builder> {
  public static final ProtoAdapter<Phone> ADAPTER = new ProtoAdapter<Phone>(FieldEncoding.LENGTH_DELIMITED, Phone.class) {
    @Override
    public int encodedSize(Phone value) {
      return (value.number != null ? ProtoAdapter.STRING.encodedSizeWithTag(1, value.number) : 0)
              + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, Phone value) throws IOException {
      if (value.number != null) ProtoAdapter.STRING.encodeWithTag(writer, 1, value.number);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public Phone decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.number(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public Phone redact(Phone value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  };

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NUMBER = "";

  public final String number;

  public Phone(String number) {
    this(number, ByteString.EMPTY);
  }

  public Phone(String number, ByteString unknownFields) {
    super(unknownFields);
    this.number = number;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.number = number;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Phone)) return false;
    Phone o = (Phone) other;
    return equals(unknownFields(), o.unknownFields())
            && equals(number, o.number);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (number != null ? number.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (number != null) builder.append(", number=").append(number);
    return builder.replace(0, 2, "Phone{").append('}').toString();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Phone, Builder> {
    public String number;

    public Builder() {
    }

    public Builder number(String number) {
      this.number = number;
      return this;
    }

    @Override
    public Phone build() {
      return new Phone(number, buildUnknownFields());
    }
  }
}
