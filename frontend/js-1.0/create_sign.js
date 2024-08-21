window.signHash = async function (hashBase64OrHex, thumbprint) {

    // Получаю нужный сертификат
    try {
        var oStore = await cadesplugin.CreateObjectAsync("CAdESCOM.Store");
        await oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE, cadesplugin.CAPICOM_MY_STORE,
            cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);

        var all_certs = await oStore.Certificates;
        var oCerts = await all_certs.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, thumbprint);
        if (await oCerts.Count === 0) {
            alert("Certificate not found");
            return;
        }

        var certificate = await oCerts.Item(1);
        oStore.Close();
        console.log("certificate!!!: ", certificate);

        let certPublicKey = await certificate.PublicKey();
        let certAlgorithm = await certPublicKey.Algorithm;
        let algorithmValue = await certAlgorithm.Value;

        // Создаем объект CAdESCOM.HashedData
        var oHashObject = await cadesplugin.CreateObjectAsync("CAdESCOM.HashedData");
        //определяем алгоритм подписания по данным из сертификата и получаем алгоритм хеширования
        if (algorithmValue === "1.2.643.7.1.1.1.1") {
            oHashObject.propset_Algorithm(cadesplugin.CADESCOM_HASH_ALGORITHM_CP_GOST_3411_2012_256);
        } else if (algorithmValue === "1.2.643.7.1.1.1.2") {
            oHashObject.propset_Algorithm(cadesplugin.CADESCOM_HASH_ALGORITHM_CP_GOST_3411_2012_512);
        } else if (algorithmValue === "1.2.643.2.2.19") {
            oHashObject.propset_Algorithm(cadesplugin.CADESCOM_HASH_ALGORITHM_CP_GOST_3411);
        } else {
            alert("Реализуемый алгоритм не подходит для подписания документа.");
            return;
        }

        await oHashObject.SetHashValue(hashBase64OrHex);

        //Получаю подписчика
        var oSigner = await cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner");
        console.log("oSigner!!!: ", oSigner);
        await oSigner.propset_Certificate(certificate);
        await oSigner.propset_CheckCertificate(true);
        await oSigner.propset_Options(cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_WHOLE_CHAIN);

        //Создание объекта для подписанных данных
        var oSignedData = await cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
        await oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);

        //Подписание данных
        // var signedMessage = await oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, /*detached*/ true);
        var sSignedMessage = await oSignedData.SignHash(oHashObject, oSigner, cadesplugin.CADESCOM_CADES_BES);
        console.log("signedMessage: ", sSignedMessage);

        var bVerifyResult = await VerifySignature(oHashObject, sSignedMessage);
        if (bVerifyResult) {
            alert("Подпись подтверждена");
        }


        return sSignedMessage;
    } catch (err) {
        console.log("exception!!! ", err);
        alert('Exception found');
        throw "Failed to signed given hash: " + err;
    }
}

async function VerifySignature(oHashedData, sSignedMessage) {
    // Создаем объект CAdESCOM.CadesSignedData
    var oSignedData = await cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");

    // Проверяем подпись
    try {
        await oSignedData.VerifyHash(oHashedData, sSignedMessage, cadesplugin.CADESCOM_CADES_BES);
    } catch (err) {
        alert("Failed to verify signature. Error: " + cadesplugin.getLastError(err));
        return false;
    }

    return true;
}

window.createSign = async function (dataToSign, thumbprint) {

    // Получаю нужный сертификат
    try {
        var oStore = await cadesplugin.CreateObjectAsync("CAdESCOM.Store");
        await oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE, cadesplugin.CAPICOM_MY_STORE,
            cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);

        var all_certs = await oStore.Certificates;
        var oCerts = await all_certs.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, thumbprint);
        if (await oCerts.Count === 0) {
            alert("Certificate not found");
            return;
        }

        var certificate = await oCerts.Item(1);
        console.log("certificate!!!: ", certificate);
    } catch (err) {
        alert('Certificate not found');
        return;
    }

    //Получаю подписчика
    try {
        var oSigner = await cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner");
        console.log("oSigner!!!: ", oSigner);
        await oSigner.propset_Certificate(certificate);
        await oSigner.propset_CheckCertificate(true);
    } catch (err) {
        throw "Failed to create CAdESCOM.CPSigner: " + err.number;
    }

    // Подписываю данные base64 с сервера. Получаю base64 - отдаю его на сервер для дальнейшей вставки
    var signedData;

    try {
        //Создание объекта для подписанных данных
        var oSignedData = await cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
        console.log("oSignedData ", oSignedData);
        //Установка кодитровки содержимого
        await oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);
        //проверка кодировки dataToSign
        //TODO
        //Установка содержимого PDF для подписи
        await oSignedData.propset_Content(dataToSign);
        console.log("set content data sign!!!")

        //Подписание данных
        var signedMessage = await oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, /*detached*/ false);
        console.log("signedMessage: ", signedMessage);

        // Получение подписанных данных
        signedData = await oSignedData.Content; // Получаем подписанные данные
        new Promise((resolve) => {
            setTimeout(() => {
                resolve('Окончательный результат', 1000);
            });
        });

        // Сохранение в файл
        // saveToFile(signedMessage, "signedDocument.p7s");
    } catch (err) {
        throw "Не удалось создать подпись из-за ошибки: " + cadesplugin.getLastError(err);
    }

    //return first string
    // var signedMessageBase64 = btoa(String.fromCharCode.apply(null, new Uint8Array(signedMessage)));
    return signedMessage;
}

// Функция для сохранения подписанного PDF
function saveToFile(data, filename) {

    // Создание ссылки для скачивания
    const link = document.createElement('a');
    link.href = 'data:application/pdf;base64,' + data;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}