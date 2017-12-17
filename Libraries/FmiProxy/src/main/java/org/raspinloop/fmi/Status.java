/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.raspinloop.fmi;

public enum Status implements org.apache.thrift.TEnum {
  OK(0),
  Warning(1),
  Discard(2),
  Error(3),
  Fatal(4),
  Pending(5);

  private final int value;

  private Status(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static Status findByValue(int value) { 
    switch (value) {
      case 0:
        return OK;
      case 1:
        return Warning;
      case 2:
        return Discard;
      case 3:
        return Error;
      case 4:
        return Fatal;
      case 5:
        return Pending;
      default:
        return null;
    }
  }
}
