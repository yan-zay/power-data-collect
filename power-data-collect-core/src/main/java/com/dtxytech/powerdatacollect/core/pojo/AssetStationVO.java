package com.dtxytech.powerdatacollect.core.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zay
 * @Date 2026/1/8 17:26
 */
@Data
public class AssetStationVO implements Serializable {

    private static final long serialVersionUID = 1L;
    //    @ApiModelProperty("资产属性表id")
    private String id;
    //    @ApiModelProperty("所属资产模型表ID")
    private String assetId;
    //    @ApiModelProperty("属性ID")
    private String indexId;
    //    @ApiModelProperty("属性名称")
    private String name;
    //    @ApiModelProperty("是否只读")
    private Boolean readOnly;
    //    @ApiModelProperty("默认值")
    private DefaultValue defaultValue;
    //    @ApiModelProperty("属性编码")
    private String code;
    //    @ApiModelProperty("实际值")
    private String value;
    //    @ApiModelProperty("字典编码")
    private String enumCode;
    //    @ApiModelProperty("指标数据类型")
    private String dataType;


    @Data
    public static class DefaultValue implements Serializable {
        private static final long serialVersionUID = 1L;
        /*        @ApiModelProperty(
                        value = "指标数据类型",
                        required = true
                )*/
        private String dataType;
        /*        @ApiModelProperty(
                        value = "默认值",
                        required = true
                )*/
        private String defaultValue;
        /*        @ApiModelProperty(
                        value = "字典编码",
                        required = true
                )*/
        private String enumCode;
        //        @ApiModelProperty("实际值")
        private String value;
    }
}
