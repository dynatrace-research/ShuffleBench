/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package rocks.theodolite.benchmarks.commons.model.records;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class ActivePowerRecord extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -5809432381123606133L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ActivePowerRecord\",\"namespace\":\"rocks.theodolite.benchmarks.commons.model.records\",\"fields\":[{\"name\":\"identifier\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"*\"},{\"name\":\"timestamp\",\"type\":\"long\",\"doc\":\"*\"},{\"name\":\"valueInW\",\"type\":\"double\",\"doc\":\"*\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<ActivePowerRecord> ENCODER =
      new BinaryMessageEncoder<ActivePowerRecord>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<ActivePowerRecord> DECODER =
      new BinaryMessageDecoder<ActivePowerRecord>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<ActivePowerRecord> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<ActivePowerRecord> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<ActivePowerRecord> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<ActivePowerRecord>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this ActivePowerRecord to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a ActivePowerRecord from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a ActivePowerRecord instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static ActivePowerRecord fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** * */
  private java.lang.String identifier;
  /** * */
  private long timestamp;
  /** * */
  private double valueInW;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public ActivePowerRecord() {}

  /**
   * All-args constructor.
   * @param identifier *
   * @param timestamp *
   * @param valueInW *
   */
  public ActivePowerRecord(java.lang.String identifier, java.lang.Long timestamp, java.lang.Double valueInW) {
    this.identifier = identifier;
    this.timestamp = timestamp;
    this.valueInW = valueInW;
  }

  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return identifier;
    case 1: return timestamp;
    case 2: return valueInW;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: identifier = value$ != null ? value$.toString() : null; break;
    case 1: timestamp = (java.lang.Long)value$; break;
    case 2: valueInW = (java.lang.Double)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'identifier' field.
   * @return *
   */
  public java.lang.String getIdentifier() {
    return identifier;
  }


  /**
   * Sets the value of the 'identifier' field.
   * *
   * @param value the value to set.
   */
  public void setIdentifier(java.lang.String value) {
    this.identifier = value;
  }

  /**
   * Gets the value of the 'timestamp' field.
   * @return *
   */
  public long getTimestamp() {
    return timestamp;
  }


  /**
   * Sets the value of the 'timestamp' field.
   * *
   * @param value the value to set.
   */
  public void setTimestamp(long value) {
    this.timestamp = value;
  }

  /**
   * Gets the value of the 'valueInW' field.
   * @return *
   */
  public double getValueInW() {
    return valueInW;
  }


  /**
   * Sets the value of the 'valueInW' field.
   * *
   * @param value the value to set.
   */
  public void setValueInW(double value) {
    this.valueInW = value;
  }

  /**
   * Creates a new ActivePowerRecord RecordBuilder.
   * @return A new ActivePowerRecord RecordBuilder
   */
  public static rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder newBuilder() {
    return new rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder();
  }

  /**
   * Creates a new ActivePowerRecord RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new ActivePowerRecord RecordBuilder
   */
  public static rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder newBuilder(rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder other) {
    if (other == null) {
      return new rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder();
    } else {
      return new rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder(other);
    }
  }

  /**
   * Creates a new ActivePowerRecord RecordBuilder by copying an existing ActivePowerRecord instance.
   * @param other The existing instance to copy.
   * @return A new ActivePowerRecord RecordBuilder
   */
  public static rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder newBuilder(rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord other) {
    if (other == null) {
      return new rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder();
    } else {
      return new rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder(other);
    }
  }

  /**
   * RecordBuilder for ActivePowerRecord instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ActivePowerRecord>
    implements org.apache.avro.data.RecordBuilder<ActivePowerRecord> {

    /** * */
    private java.lang.String identifier;
    /** * */
    private long timestamp;
    /** * */
    private double valueInW;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.identifier)) {
        this.identifier = data().deepCopy(fields()[0].schema(), other.identifier);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.valueInW)) {
        this.valueInW = data().deepCopy(fields()[2].schema(), other.valueInW);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
    }

    /**
     * Creates a Builder by copying an existing ActivePowerRecord instance
     * @param other The existing instance to copy.
     */
    private Builder(rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.identifier)) {
        this.identifier = data().deepCopy(fields()[0].schema(), other.identifier);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timestamp)) {
        this.timestamp = data().deepCopy(fields()[1].schema(), other.timestamp);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.valueInW)) {
        this.valueInW = data().deepCopy(fields()[2].schema(), other.valueInW);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'identifier' field.
      * *
      * @return The value.
      */
    public java.lang.String getIdentifier() {
      return identifier;
    }


    /**
      * Sets the value of the 'identifier' field.
      * *
      * @param value The value of 'identifier'.
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder setIdentifier(java.lang.String value) {
      validate(fields()[0], value);
      this.identifier = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'identifier' field has been set.
      * *
      * @return True if the 'identifier' field has been set, false otherwise.
      */
    public boolean hasIdentifier() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'identifier' field.
      * *
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder clearIdentifier() {
      identifier = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'timestamp' field.
      * *
      * @return The value.
      */
    public long getTimestamp() {
      return timestamp;
    }


    /**
      * Sets the value of the 'timestamp' field.
      * *
      * @param value The value of 'timestamp'.
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder setTimestamp(long value) {
      validate(fields()[1], value);
      this.timestamp = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'timestamp' field has been set.
      * *
      * @return True if the 'timestamp' field has been set, false otherwise.
      */
    public boolean hasTimestamp() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'timestamp' field.
      * *
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder clearTimestamp() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'valueInW' field.
      * *
      * @return The value.
      */
    public double getValueInW() {
      return valueInW;
    }


    /**
      * Sets the value of the 'valueInW' field.
      * *
      * @param value The value of 'valueInW'.
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder setValueInW(double value) {
      validate(fields()[2], value);
      this.valueInW = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'valueInW' field has been set.
      * *
      * @return True if the 'valueInW' field has been set, false otherwise.
      */
    public boolean hasValueInW() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'valueInW' field.
      * *
      * @return This builder.
      */
    public rocks.theodolite.benchmarks.commons.model.records.ActivePowerRecord.Builder clearValueInW() {
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActivePowerRecord build() {
      try {
        ActivePowerRecord record = new ActivePowerRecord();
        record.identifier = fieldSetFlags()[0] ? this.identifier : (java.lang.String) defaultValue(fields()[0]);
        record.timestamp = fieldSetFlags()[1] ? this.timestamp : (java.lang.Long) defaultValue(fields()[1]);
        record.valueInW = fieldSetFlags()[2] ? this.valueInW : (java.lang.Double) defaultValue(fields()[2]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<ActivePowerRecord>
    WRITER$ = (org.apache.avro.io.DatumWriter<ActivePowerRecord>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<ActivePowerRecord>
    READER$ = (org.apache.avro.io.DatumReader<ActivePowerRecord>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.identifier);

    out.writeLong(this.timestamp);

    out.writeDouble(this.valueInW);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.identifier = in.readString();

      this.timestamp = in.readLong();

      this.valueInW = in.readDouble();

    } else {
      for (int i = 0; i < 3; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.identifier = in.readString();
          break;

        case 1:
          this.timestamp = in.readLong();
          break;

        case 2:
          this.valueInW = in.readDouble();
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










