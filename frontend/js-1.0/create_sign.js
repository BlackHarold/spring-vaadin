window.createSign = async function (dataToSign, selectedCertID) {

    // Получаю нужный сертификат
    var thumbprint = selectedCertID;

    try {
        var oStore = await cadesplugin.CreateObjectAsync("CAdESCOM.Store");
        await oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE, cadesplugin.CAPICOM_MY_STORE,
            cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);

        var all_certs = await oStore.Certificates;
        var oCerts = await all_certs.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, thumbprint);
        if ((await oCerts.Count) == 0) {
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
        // await oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);
        //проверка кодировки dataToSign

        //Установка содержимого PDF для подписи
        await oSignedData.propset_Content(dataToSign);

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
        saveToFile(signedMessage, "signedDocument.pdf");
    } catch (err) {
        throw "Не удалось создать подпись из-за ошибки: " + cadesplugin.getLastError(err);
    }

    //return first string
    var signedMessageBase64 = btoa(String.fromCharCode.apply(null, new Uint8Array(signedMessage)));
    return signedMessageBase64;
}

// Функция для сохранения подписанного PDF
function saveToFile(data, filename) {
    // Преобразование подписанного сообщения в Base64
    // var signedMessageBase64 = btoa(String.fromCharCode.apply(null, new Uint8Array(data)));

    // Создание ссылки для скачивания
    const link = document.createElement('a');
    link.href = 'data:application/pdf;base64,' + data;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}