package com.nullpointerexception.collabmode.model;

public class User {
    private int id;
    private String fullName;
    private String email;
    private int teamID;
    private String teamCode;
    private boolean isTeamOwner;

    public User(int id, String fullName, String email, int teamID, boolean isTeamOwner) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.teamID = teamID;
        this.isTeamOwner = isTeamOwner;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTeamID() {
        return teamID;
    }

    public void setTeamID(int teamID) {
        this.teamID = teamID;
    }

    public boolean isTeamOwner() {
        return isTeamOwner;
    }

    public void setTeamOwner(boolean teamOwner) {
        isTeamOwner = teamOwner;
    }

    public String getTeamCode() {
        return teamCode;
    }

    public void setTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }
}
