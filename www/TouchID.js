function TouchID() {
}

TouchID.prototype.isAvailable = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "isAvailable", []);
};

TouchID.prototype.didFingerprintDatabaseChange = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "didFingerprintDatabaseChange", []);
};

TouchID.prototype.verifyFingerprint = function (message, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "verifyFingerprint", [message]);
};
TouchID.prototype.cancelVerifyFingerprint = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "cancelVerifyFingerprint", []);
};
TouchID.prototype.verifyFingerprintWithCustomPasswordFallback = function (message, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "verifyFingerprintWithCustomPasswordFallback", [message]);
};

TouchID.prototype.verifyFingerprintWithCustomPasswordFallbackAndEnterPasswordLabel = function (message, enterPasswordLabel, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "TouchID", "verifyFingerprintWithCustomPasswordFallbackAndEnterPasswordLabel", [message, enterPasswordLabel]);
};

TouchID.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.touchid = new TouchID();
    return window.plugins.touchid;
};

cordova.addConstructor(TouchID.install);