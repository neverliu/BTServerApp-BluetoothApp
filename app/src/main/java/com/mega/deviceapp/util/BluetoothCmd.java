package com.mega.deviceapp.util;

/**
 * 蓝牙命令
 * @author Administrator
 *
 */
public interface BluetoothCmd {
	
	////////////////////////////service send to client/////////////////////

	/**send ack*/
	public static final byte SEND_ACK = 0x00;
	/**send error*/
	public static final byte SEND_ERROR = 0x0c;
	/**continue package*/
	public static final byte CONTINUE_PACKAGE = 0x0a;
	/**last package*/
	public static final byte LAST_PACKAGE = 0x0b;
	/**all data length*/
	public static final byte ALL_DATA_LENGTH = 0x0c;

	//wifi
	/**send wifi info*/
	public static final byte COMMAND_WIFI_INFO = 0x01;
	/**command link wifi :ssid , password*/
	public static final byte COMMAND_LINK_WIFI = 0x02;
	/**Command scan wifi list*/
	public static final byte COMMAND_SCAN_WIFI = 0x03;

	/** change wifi*/
	public static final byte COMMAND_CHANGE_WIFI_STATE = 0x05;
	public static final byte COMMAND_CLOSE_WIFI = 0x06;
	public static final byte COMMAND_OPEN_WIFI = 0x07;
	/**wifi status */
	public static final byte WIFI_STATE_DISABLING = 0x00;
	public static final byte WIFI_STATE_ENABLING = 0x01;
	public static final byte WIFI_STATE_DISABLED = 0x02;
	public static final byte WIFI_STATE_ENABLED = 0x03;
    public static final byte WIFI_STATE_UNKNOWN = 0x04;
    /**wifi contected status*/
    public static final byte WIFI_CONTECTED = 0x10;
    public static final byte WIFI_NOT_CONTECTED = 0x011;

	//contacts
	/**query contacts*/
	public static final byte COMMAND_QUERY_CONTACTS_LIST = 0x10;
	/**inset contact*/
	public static final byte COMMAND_INSERT_CONTACT = 0x11;
	/**delete contact**/
	public static final byte COMMAND_DELETE_CONTACT = 0x12;
	/**modify contact**/
	public static final byte COMMAND_MODIFY_CONTACT = 0x13;
	/**make a call use this contact*/
	public static final byte COMMAND_MAKE_CALL_CONTACT = 0x14;

	//data
    /**change data state*/
    public static final byte COMMAND_DATA_STATE = 0x20;
    /** data state**/
    public static final byte COMMAND_DATA_STATE_ON = 0x21;
    public static final byte COMMAND_DATA_STATE_OFF = 0x22;
    public static final byte COMMAND_DATA_STATE_NO_SIM = 0x23;

	//ringtone
	/**set ringtone*/
	public static final byte COMMAND_SET_RINGTONE = 0x30;
	/**get ringtone list*/
	public static final byte COMMAND_GET_RINGTONE_LIST = 0x31;
	/**play click ringtone item*/
	public static final byte COMMAND_PLAY_RINGTONE = 0x32;
	/**stop playing ringtone */
	public static final byte COMMAND_STOP_RINGTONE = 0x33;

	//mms
	/**query mms contacts*/
	public static final byte COMMAND_QUERY_CONTACTS_MMS = 0x40;
	/**query simgle contact all mms*/
	public static final byte COMMAND_QUERY_SINGLE_CONTACT_MMS = 0x41;
	/**send mms */
	public static final byte COMMAND_SEND_MMS = 0x42;
	/**change mms read status*/
	public static final byte COMMAND_CHANGE_READ_STATUS_MMS = 0x43;
	/**change mms request flash status*/
	public static final byte COMMAND_REQUEST_FLASH_STATUS_MMS = 0x00;
	/**change mms continue flash status*/
	public static final byte COMMAND_CONTINUE_FLASH_STATUS_MMS = 0x01;
	/**change mms continue reset status*/
	public static final byte COMMAND_CONTINUE_RESET_STATUS_MMS = 0x02;
	/**change mms DATA length*/
	public static final byte DATA_LENGTH = 0x44;
	
		//battery
	/**change battery level*/
	public static final byte BATTERY_LEVEL = 0x45;
	public static final byte BATTERY_CHARGING = 0x01;
	public static final byte BATTERY_DISCHARGING = 0x02;
	public static final byte BATTERY_NOT_CHARGING = 0x03;
	public static final byte BATTERY_FULL = 0x04;

	//sim card
	/** send sim card info command*/
	public static final byte COMMAND_SEND_SIM_CARD_INFO = 0x50;
	/**sim card ready or not*/
	public static final byte COMMAND_SIM_CARD_NOT_READY = 0x00;
	public static final byte COMMAND_SIM_CARD_READY = 0x01;
	/**sim card network type none 2G 3G 4G **/
	public static final byte COMMAND_SIM_NETWORK_TYPE_NONE = 0x00;
	public static final byte COMMAND_SIM_NETWORK_TYPE_2G= 0x01;
	public static final byte COMMAND_SIM_NETWORK_TYPE_3G = 0x02;
	public static final byte COMMAND_SIM_NETWORK_TYPE_4G = 0x03;
	/**sim card signal level*/
	public static final byte COMMAND_SIM_SIGNAL_LEVEL0 = 0x00;
	public static final byte COMMAND_SIM_SIGNAL_LEVEL1 = 0x01;
	public static final byte COMMAND_SIM_SIGNAL_LEVEL2 = 0x02;
	public static final byte COMMAND_SIM_SIGNAL_LEVEL3 = 0x03;
	public static final byte COMMAND_SIM_SIGNAL_LEVEL4 = 0x04;

	//wifi ap
	/**get wifi ap*/
	public static final byte COMMAND_GET_WIFI_AP_STATE = 0x60;
	/**change wifi ap*/
	public static final byte COMMAND_CHANGE_WIFI_AP_STATE = 0x61;
	/**set up wifi ap info**/
	public static final byte COMMAND_SET_UP_WIFI_AP_INFO = 0x62;
	/**wifi ap state on .off*/
	public static final byte COMMAND_WIFI_AP_STATE_ON = 0x00;
	public static final byte COMMAND_WIFI_AP_STATE_OFF = 0x01;


	//////////////////////////////service receiver from client////////////////////


	///////////////////////////////////参考////////////////////////////////////

	/**send phone en*/
	public static final byte SEND_PHONE_EN = 0x01;
	/**send phone cn*/
	public static final byte SEND_PHONE_CN = (byte) 0x81;
	/**send message en*/
	public static final byte SEND_MESSAGE_EN = 0x02;
	/**send message cn*/
	public static final byte SEND_MESSAGE_CN = (byte) 0x82;
	/**send time*/
	public static final byte SETTING_TIME = 0x03;
	/**send person name en*/
	public static final byte SEND_PERSON_NAME_EN = 0x04;
	/**send person name cn*/
	public static final byte SEND_PERSON_NAME_CN = (byte) 0x84;
	/**query name*/
	public static final byte QUERY_NAME = 0x05;
	/**query remind*/
	public static final byte QUERY_REMIND = 0x06;
	/**setting remind*/
	public static final byte SETTING_REMIND = 0x07;
	/**query fitting status*/
	public static final byte QUERY_FITTING_STATUS = 0x08;
	/**query fitting name*/
	public static final byte SETTING_FITTING_NAME = 0x09;
	/**query calu steps*/
	public static final byte QUERY_CALU_STEPS = 0x10;
	/**query sweet time*/
	public static final byte QUERY_SWEET_TIME = 0x0a;
	/**setting distub time*/
	public static final byte SETTING_DISTUB_TIME = 0x0b;
	/**query distub time*/
	public static final byte QUERY_DISTUB_TIME = 0x0c;
	/**query temperature*/
	public static final byte QUERY_TEMPERATURE = 0x0d;
	/**setting pedometer*/
	public static final byte SETTING_PEDOMETER = 0x0e;
	
	
	////////////////////////////附            录////////////////////////
	/**send temperature*/
	public static final byte SEND_TEMPERATURE = (byte) 0xa0;
	/**send pedometer*/
	public static final byte SEND_PEDOMETER = (byte) 0xa1;
	
	
	////////////////////////////手表到手机////////////////////////
	/**receiver ack*/
	public static final byte RECEIVER_ACK = 0x00;
	/**receiver find phone*/
	public static final byte RECEIVER_FIND_PHONE = 0x0a;
	/**receiver answer phone*/
	public static final byte RECEIVER_ANSWER_PHONE = 0x02;
	/**receiver refuse phone*/
	public static final byte RECEIVER_REFUSE_PHONE = 0x03;
	/**receiver fitting status*/
	public static final byte RECEIVER_FITTING_STATUS = 0x04;
	/**receiver sweet time*/
	public static final byte RECEIVER_SWEET_TIME = 0x06;
	/**receiver distub time*/
	public static final byte RECEIVER_DISTUB_TIME = 0x07;
	/**receiver remind*/
	public static final byte RECEIVER_REMIND = 0x08;
	/**receiver pensonal name*/
	public static final byte RECEIVER_PENSONAL_NAME = 0x09;
	/**receiver temperature*/
	public static final byte RECEIVER_TEMPERATURE = (byte) 0xa0;
	/**receiver cal steps*/
	public static final byte RECEIVER_CAL_STEPS = (byte) 0xa1;
}
