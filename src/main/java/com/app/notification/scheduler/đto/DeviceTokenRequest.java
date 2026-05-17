package com.app.notification.scheduler.đto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {


    private String fcmToken;

    private String platform;    // ANDROID | IOS | WEB

    private String deviceName;

    private Long customerId;    // Flutter gửi lên cùng với token

}