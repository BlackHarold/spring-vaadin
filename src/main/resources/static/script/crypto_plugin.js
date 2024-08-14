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

window.iterateContainers = function iterateContainers() {
    return new Promise(function (resolve, reject) {
        cadesplugin.async_spawn(function* (args) {
            debugger;
            try {
                var oStore = yield cadesplugin.CreateObjectAsync("CAdESCOM.Store");
                yield oStore.Open(
                    cadesplugin.CADESCOM_CONTAINER_STORE,
                    cadesplugin.CAPICOM_MY_STORE,
                    cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);

                var oCertificates = yield oStore.Certificates;
                console.log("oCertificates");
                console.log(oCertificates);
                var count = yield oCertificates.Count;
                console.log("count", count);

                console.log("iterate certificates:")
                for (var i = 1; i <= count; i++) {
                    var cert = yield oCertificates.Item(i);
                    console.log("cert", cert);
                    try {
                        var pKey = yield cert.PrivateKey;
                        console.log("pKey: ", pKey);
                    } catch (err) {
                        alert(err)
                        continue;
                    }
                    var containerName = yield pKey.ContainerName;
                    var uniqueContainerName = yield pKey.UniqueContainerName;
                    console.log("name: ", containerName, ", unique name: ", uniqueContainerName);
                }
            } catch (err) {
                alert(cadesplugin.getLastError(err));
            }
        }, resolve, reject);
    });
}

function SignCreate(certSubjectName, dataToSign) {
    return new Promise(function (resolve, reject) {
        cadesplugin.async_spawn(function* (args) {
            var oStore = yield cadesplugin.CreateObjectAsync("CAdESCOM.Store");
            console.log("oStore", oStore);
            yield oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE, cadesplugin.CAPICOM_MY_STORE,
                cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
            var oStoreCerts = yield oStore.Certificates;
            console.log("oStoreCerts", oStoreCerts);
            var oCertificates = yield oStoreCerts.Find(
                cadesplugin.CAPICOM_CERTIFICATE_FIND_SUBJECT_NAME, certSubjectName);
            console.log("oStoreCertificates", oCertificates);
            var certsCount = yield oCertificates.Count;
            console.log("certs count", certsCount);
            if (certsCount === 0) {
                err = "Certificate not found: " + certSubjectName;
                alert(err);
                args[1](err);
            }
            var oCertificate = yield oCertificates.Item(1);
            console.log("oCertificate", oCertificate);
            var oSigner = yield cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner");
            console.log("oSigner", oSigner);

            yield oSigner.propset_Certificate(oCertificate);
            yield oSigner.propset_CheckCertificate(true);
            // yield oSigner.propset_TSAAddress("http://testca2012.cryptopro.ru/tsp/tsp.srf");
            yield oSigner.propset_TSAAddress("http://cryptopro.ru/tsp/tsp.srf");
            console.log("TSAAddress");

            var oSignedData = yield cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
            yield oSignedData.propset_Content(dataToSign);
            console.log("oSignedData", oSignedData);

            try {
                var sSignedMessage = yield oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_X_LONG_TYPE_1);
                console.log("sSigned Message", sSignedMessage);
            } catch (e) {
                console.error("exception finished!!! ", e);
                err = cadesplugin.getLastError(e);
                alert("Failed to create signature. Error: " + err);
                args[1](err);
            }

            yield oStore.Close();
            console.log("and return: ", sSignedMessage)
            return args[0](sSignedMessage);
        }, resolve, reject);
    });
}

function Verify(sSignedMessage) {
    return new Promise(function (resolve, reject) {
        cadesplugin.async_spawn(function* (args) {
            var oSignedData = yield cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
            try {
                yield oSignedData.VerifyCades(sSignedMessage, cadesplugin.CADESCOM_CADES_X_LONG_TYPE_1);
            } catch (e) {
                err = cadesplugin.getLastError(e);
                alert("Failed to verify signature. Error: " + err);
                return args[1](err);
            }
            return args[0]();
        }, resolve, reject);
    });
}

window.createAndVerifySignature = function createAndVerifySignature() {
    var oCertName = document.getElementById("CertName");
    if (oCertName == undefined) {
        console.log("== undefined")
        oCertName = 'ef42a64e0-e12e-9afb-0270-29d746cbf02';
    }

    if (oCertName === 'undefined') {
        console.log('=== undefined');
    }

    var sCertName = oCertName.value; // Здесь следует заполнить SubjectName сертификата
    console.log(sCertName);
    if (sCertName == undefined) {
        console.log("== undefined value")
        sCertName = 'Петр Петров';
    }

    if ("" === sCertName) {
        alert("Введите имя сертификата (CN).");
        return;
    }

    SignCreate(sCertName, "Message").then(
        function (signedMessage) {
            console.log("signedMessage", signedMessage);
            document.getElementById("signature").innerHTML = signedMessage;
            Verify(signedMessage).then(
                function () {
                    alert("Signature verified");
                },
                function (err) {
                    console.log("inner err ", err)
                    document.getElementById("signature").innerHTML = err;
                });
        },
        function (err) {
            console.log("outer err ", err)
            document.getElementById("signature").innerHTML = err;
        }
    );
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

window.loadCertificates = function () {
    console.log("cadesplugin", cadesplugin)
    if (typeof window.cadesplugin !== 'undefined') {
        console.log("window.cadesplugin, ", window.cadesplugin);
        return window.cadesplugin.CreateObjectAsync("CAdESCOM.CPStore")
            .then(function (store) {
                console.log("return store.Open");
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
                console.log("return certList: ", certList)
                return certList;
            })
            .catch(function (error) {
                console.error("Ошибка при загрузке сертификатов: ", error);
                throw error;
            });
        console.log("CryptoPro плагин загружен, ", cadesplugin);
    } else {
        console.error("CryptoPro плагин не найден");
    }
}