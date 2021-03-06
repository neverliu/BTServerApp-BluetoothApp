package com.mega.btserverapp.model;

/**
 * Contacts info : name , phonenumber, id
 */
public class Contacts {

    private String name;
    private String phoneNumber;
    private boolean isChecked;
    private int id;

    public boolean isChecked() {
        return isChecked;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Contacts(){}

    public Contacts(String name, String phoneNumber, int id){
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.id = id;
    }

    public Contacts(String name, String phoneNumber, boolean isChecked){
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isChecked = isChecked;
    }

    public Contacts(String name, String phoneNumber, boolean isChecked, int id){
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isChecked = isChecked;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean getIsChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void changeChecked(){
        if (isChecked){
            isChecked = false;
        }else{
            isChecked = true;
        }
    }
}
