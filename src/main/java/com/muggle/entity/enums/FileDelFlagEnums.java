package com.muggle.entity.enums;

public enum FileDelFlagEnums {
  DEL_REAL(-1, "彻底删除"),
  DEL(0, "删除"),
  RECYCLE(1, "回收站"),
  USING(2, "使用中");

    private final Integer flag;
  private final String desc;

  FileDelFlagEnums(Integer flag, String desc) {
    this.flag = flag;
    this.desc = desc;
  }

  public Integer getFlag() {
    return flag;
  }

  public String getDesc() {
    return desc;
  }
}
