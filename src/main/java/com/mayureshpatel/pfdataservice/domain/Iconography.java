package com.mayureshpatel.pfdataservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Iconography {

    private String icon;
    private String color;
    private String iconPath;

    public Iconography(String icon, String color) {
        this.icon = icon;
        this.color = color;
    }
}
