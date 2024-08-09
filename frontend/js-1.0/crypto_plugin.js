let canAsync;

window.oAbout = async function () {
    var oAbout = cadesplugin.CreateObjectAsync("CAdESCOM.About");
    console.log("oAbout ", oAbout);

    var pluginVersion = await oAbout.then((about) => {
        return about.Version;
    });
    console.log("pluginVersion ", pluginVersion);

    return pluginVersion;
};

window.certList = function run() {
    // Проверка на наличие cadesplugin
    if (!cadesplugin) {
        alert("cadesplugin не доступен. Убедитесь, что плагин установлен.");
        return;
    }

    canAsync = !!cadesplugin.CreateObjectAsync;
    console.log("canAsync " + canAsync);

    let oStore, ret;
    if (canAsync) {
        return cadesplugin.then(function () {
            return cadesplugin.CreateObjectAsync("CAPICOM.Store");
        }).then(store => {
            oStore = store;
            return oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE,
                cadesplugin.CAPICOM_MY_STORE,
                cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
        }).then(() => {
            console.log("type of ", typeof oStore.Open);
            ret = fetchCertsFromStore(oStore);
            console.log("ret: ", ret);
            return fetchCertsFromStore(oStore);
        });
    }

    #certificatesSelect
    return ret;
}

function fetchCertsFromStore(oStore, skipIds = []) {
    if (canAsync) {
        let oCertificates;
        return oStore.Certificates.then(certificates => {
            oCertificates = certificates;
            return certificates.Count;
        }).then(count => {
            const certs = [];
            for (let i = 1; i <= count; i++) certs.push(oCertificates.Item(i));
            return Promise.all(certs);
        }).then(certificates => {
            const certs = [];
            for (let i in certificates) certs.push(
                certificates[i].SubjectName,
                certificates[i].Thumbprint,
                certificates[i].ValidFromDate,
                certificates[i].ValidToDate
            );
            return Promise.all(certs);
        }).then(data => {
            const certs = [];
            for (let i = 0; i < data.length; i += 4) {
                const id = data[i + 1];
                if (skipIds.indexOf(id) + 1) break;
                const oDN = string2dn(data[i]);
                certs.push({
                    id,
                    name: formatCertificateName(oDN),
                    subject: oDN,
                    validFrom: new Date(data[i + 2]),
                    validTo: new Date(data[i + 3])
                });
            }
            return certs;
        });
    } else {
        const oCertificates = oStore.Certificates;
        const certs = [];
        for (let i = 1; i <= oCertificates.Count; i++) {
            const oCertificate = oCertificates.Item(i);
            const id = oCertificate.Thumbprint;
            if (skipIds.indexOf(id) + 1) break;
            const oDN = string2dn(oCertificate.SubjectName);
            certs.push({
                id,
                name: formatCertificateName(oDN),
                subject: oDN,
                validFrom: new Date(oCertificate.ValidFromDate),
                validTo: new Date(oCertificate.ValidToDate)
            });
        }
        return certs;
    }
}

/**
 * Разобрать субъект в объект DN
 * @param {string} subjectName
 * @returns {DN}
 */
function string2dn(subjectName) {
    const dn = new DN;
    let pairs = subjectName.match(/([а-яёА-ЯЁa-zA-Z0-9\.\s]+)=(?:("[^"]+?")|(.+?))(?:,|$)/g);
    if (pairs) pairs = pairs.map(el => el.replace(/,$/, ''));
    else pairs = []; //todo: return null?
    pairs.forEach(pair => {
        const d = pair.match(/([^=]+)=(.*)/);
        if (d && d.length === 3) {
            const rdn = d[1].trim().replace(/^OID\./, '');
            dn[rdn] = d[2].trim()
                .replace(/^"(.*)"$/, '$1')
                .replace(/""/g, '"');
        }
    });
    return convertDN(dn);
}

function DN() {
}

DN.prototype.toString = function () {
    let ret = '';
    for (let i in this) {
        if (this.hasOwnProperty(i)) {
            ret += i + '="' + this[i].replace(/"/g, '\'') + '", ';
        }
    }
    return ret;
};

function convertDN(dn) {
    const result = new DN;
    for (const field of Object.keys(dn)) {
        const oid = oids.find(item => item.oid == field || item.full == field);
        if (oid) {
            result[oid.short] = dn[field];
        } else {
            result[field] = dn[field];
        }
    }
    return result;
}

const oids = [
    {oid: '1.2.643.3.131.1.1', short: 'INN', full: 'ИНН'},
    {oid: '1.2.643.100.4', short: 'INNLE', full: 'ИНН ЮЛ'},
    {oid: '1.2.643.100.1', short: 'OGRN', full: 'ОГРН'},
    {oid: '1.2.643.100.5', short: 'OGRNIP', full: 'ОГРНИП'},
    {oid: '1.2.643.100.3', short: 'SNILS', full: 'СНИЛС'},
    {oid: '1.2.840.113549.1.9.1', short: 'E', full: 'emailAddress'},
    {oid: '2.5.4.3', short: 'CN', full: 'commonName'},
    {oid: '2.5.4.4', short: 'SN', full: 'surname'},
    {oid: '2.5.4.42', short: 'G', full: 'givenName'},
    {oid: '2.5.4.6', short: 'C', full: 'countryName'},
    {oid: '2.5.4.7', short: 'L', full: 'localityName'},
    {oid: '2.5.4.8', short: 'S', full: 'stateOrProvinceName'},
    {oid: '2.5.4.9', short: 'STREET', full: 'streetAddress'},
    {oid: '2.5.4.10', short: 'O', full: 'organizationName'},
    {oid: '2.5.4.11', short: 'OU', full: 'organizationalUnitName'},
    {oid: '2.5.4.12', short: 'T', full: 'title'},
//  { oid: '2.5.4.16',             short: '?',      full: 'postalAddress' },
];

/**
 * Получить название сертификата
 * @param {DN} o объект, включающий в себя значения субъекта сертификата
 * @see convertDN
 * @returns {String}
 */
function formatCertificateName(o) {
    return '' + o['CN']
        + (o['INNLE'] ? '; ИНН ЮЛ ' + o['INNLE'] : '')
        + (o['INN'] ? '; ИНН ' + o['INN'] : '')
        + (o['SNILS'] ? '; СНИЛС ' + o['SNILS'] : '');
}

window.loadCertificates = async function loadCertificates() {
    try {
        // Проверка на наличие cadesplugin
        if (!cadesplugin) {
            alert("cadesplugin не доступен. Убедитесь, что плагин установлен.");
            return;
        }

        //проверить async для cadesplugin
        const canAsync = !!cadesplugin.CreateObjectAsync;
        console.log("canAsync " + canAsync);

        // Инициализация cadesplugin
        const oStore = await cadesplugin.CreateObjectAsync("CAPICOM.Store");
        console.log("store ", oStore);

        //const signerOptions = cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_WHOLE_CHAIN;
        const signerOptions = cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_END_ENTITY_ONLY;
        console.log("signerOptions ", signerOptions);

        var oAbout = cadesplugin.CreateObjectAsync("CAdESCOM.About");
        console.log("oAbout ", oAbout);

        var pluginVersion = await oAbout.then((about) => {
            return about.Version;
        });
        console.log("pluginVersion ", pluginVersion);
        // <div id="indicator">Загрузка плагина...</div>
        if (pluginVersion) {
            var indicator = document.getElementById("indicator");
            indicator.style.backgroundColor = "rgba(144, 238, 144, 0.7)";
            indicator.innerText = 'Версия плагина: ' + pluginVersion;
        }

        var awaitCert = cadesplugin.CreateObjectAsync("CAdESCOM.Certificate");
        console.log("await cert ", awaitCert);
        var certificates = await awaitCert.then((r_cert) => {
            return r_cert;
        });
        console.log("cert ", certificates);

        // var rawCerts = oStore.Certificates;
        // console.log("rawCerts", rawCerts);

        // function Common_CheckForPlugIn() {
        cadesplugin.set_log_level(cadesplugin.LOG_LEVEL_DEBUG);
        if (canAsync) {
            debugger;
            include_async_code().then(function () {
                return CheckForPlugIn_Async();
            });
        } else {
            return CheckForPlugIn_NPAPI();
        }

        // Получение хранилища сертификатов
        // await store.Open(2, "", 0); // 2 - CAPICOM_CURRENT_USER_STORE

        // Получение сертификатов
        // const certificates = await store.Certificates;

        // Получение элемента select
        const selectBox = document.getElementById("certificatesSelect");
        // selectBox.innerHTML = ""; // Очистка предыдущих значений

        console.log("certificates.Count ", certificates.Count);
        // Заполнение select с сертификатами
        for (let i = 1; i <= certificates.Count; i++) {
            const cert = await certificates.Item(i);
            console.log("cert ", i, cert)
            const option = document.createElement("option");
            option.value = await cert.Thumbprint; // Используем отпечаток сертификата как значение
            option.text = await cert.SubjectName; // Используем имя субъекта как текст
            console.log("value ", value, "text ", text);
            selectBox.appendChild(option);
        }
    } catch (error) {
        console.error("Ошибка при загрузке сертификатов:", error);
        alert("Ошибка при загрузке сертификатов: " + error.message);
    }
}

var async_code_included = 0;
var async_Promise;
var async_resolve;

function include_async_code() {
    if (async_code_included) {
        return async_Promise;
    }
    var fileref = document.createElement('script');
    fileref.setAttribute("type", "text/javascript");
    fileref.setAttribute("src", "../script/async_code.js?v=271851");
    document.getElementsByTagName("head")[0].appendChild(fileref);
    async_Promise = new Promise(function (resolve, reject) {
        async_resolve = resolve;
    });
    async_code_included = 1;
    return async_Promise;
}

function getCertificateObject(certThumbprint, pin) {
    if(canAsync) {
        let oStore, oCertificate;
        return cadesplugin
            .then(() => cadesplugin.CreateObjectAsync("CAPICOM.Store")) //TODO: CADESCOM.Store ?
            .then(o => {
                oStore = o;
                return oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE,
                    cadesplugin.CAPICOM_MY_STORE,
                    cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
            })
            .then(() => findCertInStore(oStore, certThumbprint))
            .then(cert => oStore.Close().then(() => {
                if (!cert && hasContainerStore()) return oStore.Open(cadesplugin.CADESCOM_CONTAINER_STORE)
                    .then(() => findCertInStore(oStore, certThumbprint))
                    .then(c => oStore.Close().then(() => c));
                else return cert;
            }))
            .then(certificate => {
                if(!certificate) {
                    throw new Error("Не обнаружен сертификат c отпечатком " + certThumbprint);
                }
                return oCertificate = certificate;
            })
            .then(() => oCertificate.HasPrivateKey())
            .then(hasKey => {
                let p = Promise.resolve();
                if (hasKey && pin) {
                    p = p.then(() => oCertificate.PrivateKey).then(privateKey => Promise.all([
                        privateKey.propset_KeyPin(pin ? pin : ''),
                        privateKey.propset_CachePin(binded)
                    ]));
                }
                return p;
            })
            .then(() => oCertificate);
    }
    else {
        let oCertificate;
        const oStore = cadesplugin.CreateObject("CAPICOM.Store");
        oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE,
            cadesplugin.CAPICOM_MY_STORE,
            cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
        oCertificate = findCertInStore(oStore, certThumbprint);
        oStore.Close();

        if (!oCertificate && hasContainerStore()) {
            oStore.Open(cadesplugin.CADESCOM_CONTAINER_STORE);
            oCertificate = findCertInStore(oStore, certThumbprint);
            oStore.Close();
        }

        if(!oCertificate) {
            throw new Error("Не обнаружен сертификат c отпечатком " + certThumbprint);
        }

        if (oCertificate.HasPrivateKey && pin) {
            oCertificate.PrivateKey.KeyPin = pin ? pin : '';
            if(oCertificate.PrivateKey.CachePin !== undefined) {
                // возможно не поддерживается в ИЕ
                // https://www.cryptopro.ru/forum2/default.aspx?g=posts&t=10170
                oCertificate.PrivateKey.CachePin = binded;
            }
        }
        return oCertificate;
    }
}

/**
 * Подпись данных отсоединенная или присоединенная
 * @param {string} dataBase64
 * @param {string} certThumbprint
 * @param {object} [options]
 * @param {string} [options.pin] будет запрошен, если отсутствует
 * @param {boolean} [options.attached] присоединенная подпись
 * @returns {Promise<string>} base64
 */
window.signData = function(dataBase64, certThumbprint, options){
    if (typeof options === 'string') {
        // обратная совместимость с версией 2.3
        options = { pin: options };
    }
    if (!options) options = {};
    const { pin, attached } = options;
    if(canAsync) {
        let oCertificate, oSigner, oSignedData;
        return getCertificateObject(certThumbprint, pin)
            .then(certificate => {
                oCertificate = certificate;
                return Promise.all([
                    cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner"),
                    cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData")
                ]);
            })
            .then(objects => {
                oSigner = objects[0];
                oSignedData = objects[1];
                return Promise.all([
                    oSigner.propset_Certificate(oCertificate),
                    oSigner.propset_Options(signerOptions),
                    // Значение свойства ContentEncoding должно быть задано до заполнения свойства Content
                    oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY)
                ]);
            })
            .then(() => oSignedData.propset_Content(dataBase64))
            .then(() => oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, !attached))
            .catch(e => {
                const err = getError(e);
                throw new Error(err);
            });
    }
    else {
        return new Promise(resolve => {
            try {
                const oCertificate = getCertificateObject(certThumbprint, pin);
                const oSigner = cadesplugin.CreateObject("CAdESCOM.CPSigner");
                oSigner.Certificate = oCertificate;
                oSigner.Options = signerOptions;

                const oSignedData = cadesplugin.CreateObject("CAdESCOM.CadesSignedData");
                // Значение свойства ContentEncoding должно быть задано до заполнения свойства Content
                oSignedData.ContentEncoding = cadesplugin.CADESCOM_BASE64_TO_BINARY;
                oSignedData.Content = dataBase64;

                const sSignedMessage = oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, !attached);
                resolve(sSignedMessage);
            }
            catch (e) {
                const err = getError(e);
                throw new Error(err);
            }
        });
    }
};

function run() {
    cadesplugin.async_spawn(function* (args) {
        // Проверяем, работает ли File API
        if (window.FileReader) {
            // Браузер поддерживает File API.
        } else {
            alert('The File APIs are not fully supported in this browser.');
        }
        if (0 === document.getElementById("uploadFile").files.length) {
            alert("Select the file.");
            return;
        }
        var oFile = document.getElementById("uploadFile").files[0];
        var oFReader = new FileReader();

        if (typeof (oFReader.readAsDataURL) != "function") {
            alert("Method readAsDataURL() is not supported in FileReader.");
            return;
        }

        oFReader.readAsDataURL(oFile);

        oFReader.onload = function (oFREvent) {
            cadesplugin.async_spawn(function* (args) {
                var header = ";base64,";
                var sFileData = oFREvent.target.result;
                var sBase64Data = sFileData.substr(sFileData.indexOf(header) + header.length);

                var oCertName = document.getElementById("CertName");
                var sCertName = oCertName.value; // Здесь следует заполнить SubjectName сертификата
                if ("" == sCertName) {
                    alert("Введите имя сертификата (CN).");
                    return;
                }
                var oStore = yield cadesplugin.CreateObjectAsync("CAdESCOM.Store");
                yield oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE, cadesplugin.CAPICOM_MY_STORE,
                    cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);

                var oStoreCerts = yield oStore.Certificates;
                var oCertificates = yield oStoreCerts.Find(
                    cadesplugin.CAPICOM_CERTIFICATE_FIND_SUBJECT_NAME, sCertName);
                var certsCount = yield oCertificates.Count;
                if (certsCount === 0) {
                    alert("Certificate not found: " + sCertName);
                    return;
                }
                var oCertificate = yield oCertificates.Item(1);
                var oSigner = yield cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner");
                yield oSigner.propset_Certificate(oCertificate);
                yield oSigner.propset_CheckCertificate(true);

                var oSignedData = yield cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
                yield oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);
                yield oSignedData.propset_Content(sBase64Data);

                try {
                    var sSignedMessage = yield oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, true);
                } catch (err) {
                    alert("Failed to create signature. Error: " + cadesplugin.getLastError(err));
                    return;
                }

                yield oStore.Close();

                // Выводим отделенную подпись в BASE64 на страницу
                // Такая подпись должна проверяться в КриптоАРМ и cryptcp.exe
                document.getElementById("signature").innerHTML = sSignedMessage;

                var oSignedData2 = yield cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData");
                try {
                    yield oSignedData2.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY);
                    yield oSignedData2.propset_Content(sBase64Data);
                    yield oSignedData2.VerifyCades(sSignedMessage, cadesplugin.CADESCOM_CADES_BES, true);
                    alert("Signature verified");
                } catch (err) {
                    alert("Failed to verify signature. Error: " + cadesplugin.getLastError(err));
                    return;
                }
            });
        };
    });
}