/**
 * Autogenerated by Thrift Compiler (0.9.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.hadoop.hive.metastore.api;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Device implements org.apache.thrift.TBase<Device, Device._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Device");

  private static final org.apache.thrift.protocol.TField DEVID_FIELD_DESC = new org.apache.thrift.protocol.TField("devid", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField PROP_FIELD_DESC = new org.apache.thrift.protocol.TField("prop", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField NODE_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("node_name", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField STATUS_FIELD_DESC = new org.apache.thrift.protocol.TField("status", org.apache.thrift.protocol.TType.I32, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new DeviceStandardSchemeFactory());
    schemes.put(TupleScheme.class, new DeviceTupleSchemeFactory());
  }

  private String devid; // required
  private int prop; // required
  private String node_name; // required
  private int status; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    DEVID((short)1, "devid"),
    PROP((short)2, "prop"),
    NODE_NAME((short)3, "node_name"),
    STATUS((short)4, "status");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // DEVID
          return DEVID;
        case 2: // PROP
          return PROP;
        case 3: // NODE_NAME
          return NODE_NAME;
        case 4: // STATUS
          return STATUS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __PROP_ISSET_ID = 0;
  private static final int __STATUS_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.DEVID, new org.apache.thrift.meta_data.FieldMetaData("devid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.PROP, new org.apache.thrift.meta_data.FieldMetaData("prop", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    tmpMap.put(_Fields.NODE_NAME, new org.apache.thrift.meta_data.FieldMetaData("node_name", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.STATUS, new org.apache.thrift.meta_data.FieldMetaData("status", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I32)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Device.class, metaDataMap);
  }

  public Device() {
  }

  public Device(
    String devid,
    int prop,
    String node_name,
    int status)
  {
    this();
    this.devid = devid;
    this.prop = prop;
    setPropIsSet(true);
    this.node_name = node_name;
    this.status = status;
    setStatusIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Device(Device other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetDevid()) {
      this.devid = other.devid;
    }
    this.prop = other.prop;
    if (other.isSetNode_name()) {
      this.node_name = other.node_name;
    }
    this.status = other.status;
  }

  public Device deepCopy() {
    return new Device(this);
  }

  @Override
  public void clear() {
    this.devid = null;
    setPropIsSet(false);
    this.prop = 0;
    this.node_name = null;
    setStatusIsSet(false);
    this.status = 0;
  }

  public String getDevid() {
    return this.devid;
  }

  public void setDevid(String devid) {
    this.devid = devid;
  }

  public void unsetDevid() {
    this.devid = null;
  }

  /** Returns true if field devid is set (has been assigned a value) and false otherwise */
  public boolean isSetDevid() {
    return this.devid != null;
  }

  public void setDevidIsSet(boolean value) {
    if (!value) {
      this.devid = null;
    }
  }

  public int getProp() {
    return this.prop;
  }

  public void setProp(int prop) {
    this.prop = prop;
    setPropIsSet(true);
  }

  public void unsetProp() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __PROP_ISSET_ID);
  }

  /** Returns true if field prop is set (has been assigned a value) and false otherwise */
  public boolean isSetProp() {
    return EncodingUtils.testBit(__isset_bitfield, __PROP_ISSET_ID);
  }

  public void setPropIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __PROP_ISSET_ID, value);
  }

  public String getNode_name() {
    return this.node_name;
  }

  public void setNode_name(String node_name) {
    this.node_name = node_name;
  }

  public void unsetNode_name() {
    this.node_name = null;
  }

  /** Returns true if field node_name is set (has been assigned a value) and false otherwise */
  public boolean isSetNode_name() {
    return this.node_name != null;
  }

  public void setNode_nameIsSet(boolean value) {
    if (!value) {
      this.node_name = null;
    }
  }

  public int getStatus() {
    return this.status;
  }

  public void setStatus(int status) {
    this.status = status;
    setStatusIsSet(true);
  }

  public void unsetStatus() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __STATUS_ISSET_ID);
  }

  /** Returns true if field status is set (has been assigned a value) and false otherwise */
  public boolean isSetStatus() {
    return EncodingUtils.testBit(__isset_bitfield, __STATUS_ISSET_ID);
  }

  public void setStatusIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __STATUS_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case DEVID:
      if (value == null) {
        unsetDevid();
      } else {
        setDevid((String)value);
      }
      break;

    case PROP:
      if (value == null) {
        unsetProp();
      } else {
        setProp((Integer)value);
      }
      break;

    case NODE_NAME:
      if (value == null) {
        unsetNode_name();
      } else {
        setNode_name((String)value);
      }
      break;

    case STATUS:
      if (value == null) {
        unsetStatus();
      } else {
        setStatus((Integer)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case DEVID:
      return getDevid();

    case PROP:
      return Integer.valueOf(getProp());

    case NODE_NAME:
      return getNode_name();

    case STATUS:
      return Integer.valueOf(getStatus());

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case DEVID:
      return isSetDevid();
    case PROP:
      return isSetProp();
    case NODE_NAME:
      return isSetNode_name();
    case STATUS:
      return isSetStatus();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Device)
      return this.equals((Device)that);
    return false;
  }

  public boolean equals(Device that) {
    if (that == null)
      return false;

    boolean this_present_devid = true && this.isSetDevid();
    boolean that_present_devid = true && that.isSetDevid();
    if (this_present_devid || that_present_devid) {
      if (!(this_present_devid && that_present_devid))
        return false;
      if (!this.devid.equals(that.devid))
        return false;
    }

    boolean this_present_prop = true;
    boolean that_present_prop = true;
    if (this_present_prop || that_present_prop) {
      if (!(this_present_prop && that_present_prop))
        return false;
      if (this.prop != that.prop)
        return false;
    }

    boolean this_present_node_name = true && this.isSetNode_name();
    boolean that_present_node_name = true && that.isSetNode_name();
    if (this_present_node_name || that_present_node_name) {
      if (!(this_present_node_name && that_present_node_name))
        return false;
      if (!this.node_name.equals(that.node_name))
        return false;
    }

    boolean this_present_status = true;
    boolean that_present_status = true;
    if (this_present_status || that_present_status) {
      if (!(this_present_status && that_present_status))
        return false;
      if (this.status != that.status)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();

    boolean present_devid = true && (isSetDevid());
    builder.append(present_devid);
    if (present_devid)
      builder.append(devid);

    boolean present_prop = true;
    builder.append(present_prop);
    if (present_prop)
      builder.append(prop);

    boolean present_node_name = true && (isSetNode_name());
    builder.append(present_node_name);
    if (present_node_name)
      builder.append(node_name);

    boolean present_status = true;
    builder.append(present_status);
    if (present_status)
      builder.append(status);

    return builder.toHashCode();
  }

  public int compareTo(Device other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    Device typedOther = (Device)other;

    lastComparison = Boolean.valueOf(isSetDevid()).compareTo(typedOther.isSetDevid());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDevid()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.devid, typedOther.devid);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetProp()).compareTo(typedOther.isSetProp());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetProp()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.prop, typedOther.prop);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetNode_name()).compareTo(typedOther.isSetNode_name());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetNode_name()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.node_name, typedOther.node_name);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetStatus()).compareTo(typedOther.isSetStatus());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetStatus()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.status, typedOther.status);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Device(");
    boolean first = true;

    sb.append("devid:");
    if (this.devid == null) {
      sb.append("null");
    } else {
      sb.append(this.devid);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("prop:");
    sb.append(this.prop);
    first = false;
    if (!first) sb.append(", ");
    sb.append("node_name:");
    if (this.node_name == null) {
      sb.append("null");
    } else {
      sb.append(this.node_name);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("status:");
    sb.append(this.status);
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class DeviceStandardSchemeFactory implements SchemeFactory {
    public DeviceStandardScheme getScheme() {
      return new DeviceStandardScheme();
    }
  }

  private static class DeviceStandardScheme extends StandardScheme<Device> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Device struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // DEVID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.devid = iprot.readString();
              struct.setDevidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // PROP
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.prop = iprot.readI32();
              struct.setPropIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // NODE_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.node_name = iprot.readString();
              struct.setNode_nameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // STATUS
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.status = iprot.readI32();
              struct.setStatusIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, Device struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.devid != null) {
        oprot.writeFieldBegin(DEVID_FIELD_DESC);
        oprot.writeString(struct.devid);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(PROP_FIELD_DESC);
      oprot.writeI32(struct.prop);
      oprot.writeFieldEnd();
      if (struct.node_name != null) {
        oprot.writeFieldBegin(NODE_NAME_FIELD_DESC);
        oprot.writeString(struct.node_name);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(STATUS_FIELD_DESC);
      oprot.writeI32(struct.status);
      oprot.writeFieldEnd();
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class DeviceTupleSchemeFactory implements SchemeFactory {
    public DeviceTupleScheme getScheme() {
      return new DeviceTupleScheme();
    }
  }

  private static class DeviceTupleScheme extends TupleScheme<Device> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Device struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetDevid()) {
        optionals.set(0);
      }
      if (struct.isSetProp()) {
        optionals.set(1);
      }
      if (struct.isSetNode_name()) {
        optionals.set(2);
      }
      if (struct.isSetStatus()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetDevid()) {
        oprot.writeString(struct.devid);
      }
      if (struct.isSetProp()) {
        oprot.writeI32(struct.prop);
      }
      if (struct.isSetNode_name()) {
        oprot.writeString(struct.node_name);
      }
      if (struct.isSetStatus()) {
        oprot.writeI32(struct.status);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Device struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.devid = iprot.readString();
        struct.setDevidIsSet(true);
      }
      if (incoming.get(1)) {
        struct.prop = iprot.readI32();
        struct.setPropIsSet(true);
      }
      if (incoming.get(2)) {
        struct.node_name = iprot.readString();
        struct.setNode_nameIsSet(true);
      }
      if (incoming.get(3)) {
        struct.status = iprot.readI32();
        struct.setStatusIsSet(true);
      }
    }
  }

}

