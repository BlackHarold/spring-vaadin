window.callCryptoProPlugin = function callCryptoProPlugin() {
    if (window.cryptoProPlugin) {
        // Пример вызова метода плагина
        window.cryptoProPlugin.signData("dataToSign", function (response) {
            console.log("Подпись выполнена: ", response);
            // Обработка успешной подписи
        }, function (error) {
            console.error("Ошибка при подписи: ", error);
            // Обработка ошибки
        });
    } else {
        console.error("CryptoPro плагин не найден");
    }
}



function run() {
    var ProviderName = "Crypto-Pro GOST R 34.10-2001 Cryptographic Service Provider";
    var ProviderType = 75;

    var elem = document.getElementById("ProviderName");
    var ProviderName = elem.value;

    elem = document.getElementById("ProviderType");
    var ProviderType = elem.value;

    var Version = get_version(ProviderName, ProviderType);

    elem = document.getElementById("ProviderVersion");

    if (Version)
        elem.value = Version;
}

function get_version(ProviderName, ProviderType) {
    var oVersion;
    try {
        var oAbout = cadesplugin.CreateObject("CAdESCOM.About");

        oVersion = oAbout.CSPVersion(ProviderName, parseInt(ProviderType, 10));

        var Minor = oVersion.MinorVersion;
        var Major = oVersion.MajorVersion;
        var Build = oVersion.BuildVersion;
        var Version = oVersion.toString();

        return Version;
    } catch (er) {
        if (er.message.indexOf("0x80090019") + 1)
            return "Указанный CSP не установлен";
        else
            return er.message;
        return false;
    }
}


console.log("callCryptoProPlugin loaded")

window.loadCertificates = function loadCertificates() {
    if (typeof window.cadesplugin !== 'undefined') {
        return window.cadesplugin.CreateObjectAsync("CAdESCOM.CPStore")
            .then(function (store) {
                return store.Open(cadesplugin.CADESCOM_CONTAINER_STORE);
            })
            .then(function (store) {
                var certs = store.Cards.Item(1).Certificates;
                var certList = [];
                for (var i = 1; i <= certs.Count; i++) {
                    var cert = certs.Item(i);
                    certList.push({
                        subjectName: cert.SubjectName,
                        issuerName: cert.IssuerName,
                        validFrom: cert.ValidFromDate,
                        validTo: cert.ValidToDate
                    });
                }
                return certList;
            })
            .catch(function (error) {
                console.error("Ошибка при загрузке сертификатов: ", error);
                throw error;
            });
    } else {
        console.error("CryptoPro плагин не найден");
    }
}