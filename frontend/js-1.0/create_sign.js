window.createSign = async function (dataToSign, selectedCertID) {
    debugger;
    console.log("in createSign!!!")

    // Получаю нужный сертификат
    var thumbprint = selectedCertID;

    try {
        var oStore = await cadesplugin.CreateObjectAsync("CAdESCOM.Store");
        await oStore.Open();

        var all_certs = await oStore.Certificates;
        var oCerts = await all_certs.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, thumbprint);
        if ((await oCerts.Count) == 0) {
            alert("Certificate not found");
            return;
        }

        var certificate = await oCerts.Item(1);
        console.log("certificate!!! ", certificate);
    } catch (err) {
        alert('Certificate not found');
        return;
    }

    // Хэшируем. Используем CADESCOM_BASE64_TO_BINARY, поэтому оставляем пришедший base64
    // try {
    //     var oHashedData = await cadesplugin.CreateObjectAsync('CAdESCOM.HashedData');
    //     await oHashedData.propset_Algorithm(cadesplugin.CADESCOM_HASH_ALGORITHM_CP_GOST_3411);
    //     await oHashedData.propset_DataEncoding = cadesplugin.CADESCOM_BASE64_TO_BINARY;
    //     await oHashedData.Hash(dataToSign);
    // } catch (err) {
    //     throw "Failed to create CAdESCOM.HashedData: " + err.number;
    // }

    // подписчик
    try {
        var oSigner = await cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner");
        console.log("oSigner ", oSigner);
        await oSigner.propset_Certificate(certificate);
    } catch (err) {
        throw "Failed to create CAdESCOM.CPSigner: " + err.number;
    }

    // Подписываю данные base64 с сервера. Получаю base64 - отдаю его на сервер для дальнейшей вставки
    var oSignedData = await cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
    console.log("oSignedData ", oSignedData);
    if (dataToSign) {
        var signedData;
        try {
            await oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);
            await oSignedData.propset_Content(dataToSign);
            var Signature = await oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, true);
            console.log("Signature: ", Signature);

            // Получение подписанных данных
            signedData = await oSignedData.Content; // Получаем подписанные данные
            new Promise((resolve) => {
                setTimeout(() => {
                    resolve('Окончательный результат', 1000);
                });
            });
            console.log("signedData ", signedData);
        } catch (err) {
            throw "Не удалось создать подпись из-за ошибки: " + cadesplugin.getLastError(err);
        }
    }

    return signedData;
}