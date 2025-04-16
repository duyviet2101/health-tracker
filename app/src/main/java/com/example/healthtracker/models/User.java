package com.example.healthtracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String displayName;
    private String email;
    private String dob;
    private String photoUrl;
    private String weight;
    private String height;
}
