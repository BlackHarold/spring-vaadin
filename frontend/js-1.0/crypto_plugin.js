let canAsync;
let pluginVersion = '';
let signerOptions = 0;

window.oAbout = async function () {
    var oAbout = cadesplugin.CreateObjectAsync("CAdESCOM.About");
    console.log("oAbout ", oAbout);

    pluginVersion = await oAbout.then((about) => {
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
    signerOptions = cadesplugin.CAPICOM_CERTIFICATE_INCLUDE_END_ENTITY_ONLY;

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

        pluginVersion = await oAbout.then((about) => {
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

window.getCertificate = function (certThumbprint, pin) {
    let oCertificate, oSigner, oSignedData;
    console.log("cert thumb ", certThumbprint, " pin ", pin);
    return getCertificateObject(certThumbprint, pin)
        .then(certificate => {
            oCertificate = certificate;
            console.log("oCertificate ", oCertificate);
            return Promise.all([
                cadesplugin.CreateObjectAsync("CAdESCOM.CPSigner"),
                cadesplugin.CreateObjectAsync("CAdESCOM.CadesSignedData")
            ]);
        })
        .then(objects => {
            oSigner = objects[0];
            oSignedData = objects[1];
            console.log("oSigner ", oSigner);
            console.log("oSignedData ", oSignedData);
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
};

/**
 * Получение информации о сертификате.
 * @param {string} certThumbprint
 * @param {object} [options]
 * @param {boolean} [options.checkValid] проверять валидность сертификата через СКЗИ, а не сроку действия
 * @returns {Promise<Object>}
 */
window.get_cert_info = function (certThumbprint, options) {
    if (!options) options = {
        checkValid: true
    };

    const infoToString = function () {
        return 'Название:              ' + this.Name +
            '\nИздатель:              ' + this.IssuerName +
            '\nСубъект:               ' + this.SubjectName +
            '\nВерсия:                ' + this.Version +
            '\nАлгоритм:              ' + this.Algorithm + // PublicKey Algorithm
            '\nСерийный №:            ' + this.SerialNumber +
            '\nОтпечаток SHA1:        ' + this.Thumbprint +
            '\nНе действителен до:    ' + this.ValidFromDate +
            '\nНе действителен после: ' + this.ValidToDate +
            '\nПриватный ключ:        ' + (this.HasPrivateKey ? 'Есть' : 'Нет') +
            '\nАлгоритм:              ' + this.Algorithm +
            '\nКриптопровайдер:       ' + this.ProviderName + // PrivateKey ProviderName
            '\nВалидный:              ' + (this.IsValid ? 'Да' : 'Нет');
    };

    if (canAsync) {
        let oInfo = {};
        return getCertificateObject(certThumbprint)
            .then(oCertificate => Promise.all([
                oCertificate.HasPrivateKey(),
                options.checkValid ? oCertificate.IsValid().then(v => v.Result) : undefined,
                oCertificate.IssuerName,
                oCertificate.SerialNumber,
                oCertificate.SubjectName,
                oCertificate.Thumbprint,
                oCertificate.ValidFromDate,
                oCertificate.ValidToDate,
                oCertificate.Version,
                oCertificate.PublicKey().then(k => k.Algorithm).then(a => a.FriendlyName),
                oCertificate.HasPrivateKey().then(key => !key && ['', undefined] || oCertificate.PrivateKey.then(k => Promise.all([
                    k.ProviderName, k.ProviderType
                ])))
            ]))
            .then(a => {
                oInfo = {
                    HasPrivateKey: a[0],
                    IsValid: a[1],
                    IssuerName: a[2],
                    Issuer: undefined,
                    SerialNumber: a[3],
                    SubjectName: a[4],
                    Subject: undefined,
                    Name: undefined,
                    Thumbprint: a[5],
                    ValidFromDate: new Date(a[6]),
                    ValidToDate: new Date(a[7]),
                    Version: a[8],
                    Algorithm: a[9],
                    ProviderName: a[10][0],
                    ProviderType: a[10][1]
                };
                oInfo.Subject = string2dn(oInfo.SubjectName);
                oInfo.Issuer = string2dn(oInfo.IssuerName);
                oInfo.Name = oInfo.Subject['CN'];
                if (!options.checkValid) {
                    const dt = new Date();
                    oInfo.IsValid = dt >= oInfo.ValidFromDate && dt <= oInfo.ValidToDate;
                }
                oInfo.toString = infoToString;

                console.log("oInfo ", oInfo);
                return oInfo;
            })
            .catch(e => {
                const err = getError(e);
                throw new Error(err);
            });
    } else {
        return new Promise(resolve => {
            try {
                const oCertificate = getCertificateObject(certThumbprint);
                const hasKey = oCertificate.HasPrivateKey();
                const oParsedSubj = string2dn(oCertificate.SubjectName);
                const oInfo = {
                    HasPrivateKey: hasKey,
                    IsValid: options.checkValid ? oCertificate.IsValid().Result : undefined,
                    IssuerName: oCertificate.IssuerName,
                    Issuer: string2dn(oCertificate.IssuerName),
                    SerialNumber: oCertificate.SerialNumber,
                    SubjectName: oCertificate.SubjectName,
                    Subject: oParsedSubj,
                    Name: oParsedSubj['CN'],
                    Thumbprint: oCertificate.Thumbprint,
                    ValidFromDate: new Date(oCertificate.ValidFromDate),
                    ValidToDate: new Date(oCertificate.ValidToDate),
                    Version: oCertificate.Version,
                    Algorithm: oCertificate.PublicKey().Algorithm.FriendlyName,
                    ProviderName: hasKey && oCertificate.PrivateKey.ProviderName || '',
                    ProviderType: hasKey && oCertificate.PrivateKey.ProviderType || undefined,
                };
                if (!options.checkValid) {
                    const dt = new Date();
                    oInfo.IsValid = dt >= oInfo.ValidFromDate && dt <= oInfo.ValidToDate;
                }
                oInfo.toString = infoToString;
                console.log("infoToString ", infoToString);
                resolve(oInfo);
            } catch (e) {
                const err = getError(e);
                throw new Error(err);
            }
        });
    }
};

function getCertificateObject(certThumbprint, pin) {
    if (canAsync) {
        console.log("canAsync ", canAsync);
        let oStore, oCertificate;
        return cadesplugin.then(function () {
            console.log("CreateObjectAsync");
            return cadesplugin.CreateObjectAsync("CAPICOM.Store");
        }).then(store => {
            oStore = store;
            console.log("oStore ", oStore);
            return oStore.Open(cadesplugin.CAPICOM_CURRENT_USER_STORE,
                cadesplugin.CAPICOM_MY_STORE,
                cadesplugin.CAPICOM_STORE_OPEN_MAXIMUM_ALLOWED);
        })
            .then(function () {
                console.log("findCertInStore")
                findCertInStore(oStore, certThumbprint);
            })
            .then(cert => oStore.Close().then(() => {
                console.log("oStore.Close().then");
                console.log("hasContainerStore() ", hasContainerStore());
                console.log("!cert ", !cert);
                if (!cert && hasContainerStore()) {
                    console.log("cert ", cert, " hasContainer ", hasContainerStore());
                    return oStore.Open(cadesplugin.CADESCOM_CONTAINER_STORE)
                        .then(() => findCertInStore(oStore, certThumbprint))
                        .then(c => oStore.Close().then(() => c));
                } else {
                    console.log("cert ", cert);
                    return cert;
                }
            }))
            .then(certificate => {
                if (!certificate) {
                    throw new Error("Не обнаружен сертификат c отпечатком " + certThumbprint);
                }
                console.log("certificate ", certificate);
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
            .then(function () {
                console.log("return Certificate ", oCertificate);
                return oCertificate;
            });
    } else {
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

        if (!oCertificate) {
            throw new Error("Не обнаружен сертификат c отпечатком " + certThumbprint);
        }

        if (oCertificate.HasPrivateKey && pin) {
            oCertificate.PrivateKey.KeyPin = pin ? pin : '';
            if (oCertificate.PrivateKey.CachePin !== undefined) {
                // возможно не поддерживается в ИЕ
                // https://www.cryptopro.ru/forum2/default.aspx?g=posts&t=10170
                oCertificate.PrivateKey.CachePin = binded;
            }
        }
        return oCertificate;
    }
}

function findCertInStore(oStore, certThumbprint) {
    if (canAsync) {
        return oStore.Certificates
            .then(certificates => certificates.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, certThumbprint))
            .then(certificates => certificates.Count.then(count => {
                if (count === 1) {
                    return certificates.Item(1);
                } else {
                    return null;
                }
            }));
    } else {
        const oCertificates = oStore.Certificates.Find(cadesplugin.CAPICOM_CERTIFICATE_FIND_SHA1_HASH, certThumbprint);
        if (oCertificates.Count === 1) {
            return oCertificates.Item(1);
        } else {
            return null;
        }
    }
}

function hasContainerStore() {
    //В версии плагина 2.0.13292+ есть возможность получить сертификаты из
    //закрытых ключей и не установленных в хранилище
    // но не смотря на это, все равно приходится собирать список сертификатов
    // старым и новым способом тк в новом будет отсутствовать часть старого
    // предположительно ГОСТ-2001 с какими-то определенными Extended Key Usage OID

    return versionCompare(pluginVersion, '2.0.13292') >= 0;
}

/**
 * compare function takes version numbers of any length and any number size per segment.
 * @see https://stackoverflow.com/a/16187766
 * @param {string} a
 * @param {string} b
 * @returns {number} < 0 if a < b; > 0 if a > b; 0 if a = b
 */
function versionCompare(a, b) {
    let i, diff;
    const regExStrip0 = /(\.0+)+$/;
    const segmentsA = a.replace(regExStrip0, '').split('.');
    const segmentsB = b.replace(regExStrip0, '').split('.');
    const l = Math.min(segmentsA.length, segmentsB.length);

    for (i = 0; i < l; i++) {
        diff = parseInt(segmentsA[i], 10) - parseInt(segmentsB[i], 10);
        if (diff) {
            return diff;
        }
    }

    console.log("compare version ", segmentsA.length - segmentsB.length)
    return segmentsA.length - segmentsB.length;
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
window.signData = function (dataBase64, certThumbprint, options) {
    if (typeof options === 'string') {
        // обратная совместимость с версией 2.3
        options = {pin: options};
    }
    if (!options) options = {};
    const {pin, attached} = options;
    if (canAsync) {
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
                console.log("dataBase64, " + dataBase64);
                return Promise.all([
                    oSigner.propset_Certificate(oCertificate),
                    oSigner.propset_Options(signerOptions),
                    // Значение свойства ContentEncoding должно быть задано до заполнения свойства Content
                    oSignedData.propset_ContentEncoding(cadesplugin.CADESCOM_BASE64_TO_BINARY),
                    oSignedData.propset_Content(dataBase64)
                ]);
            })
            .then(function () {
                // oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, !attached);
                var sSignedMessage = oSignedData.SignCades(oSigner, cadesplugin.CADESCOM_CADES_BES, true);
                console.log("sSignedMessage. ", sSignedMessage);
                return Promise.all([
                    oSignedData.Content,
                    oSignedData.ContentEncoding,
                    sSignedMessage
                ]);
            }).then((oSignedData) => {
                // Декодируем Base64 в бинарные данные
                console.log("signed, ", oSignedData[0], " encoding, ", oSignedData[1], "[3] ", oSignedData[2]);

                const base64Pattern = /^[A-Za-z0-9+/]+={0,2}$/;
                console.log("isBase64Data ", base64Pattern.test(oSignedData[0]));
                var byteCharacters = atob(oSignedData[0]);
                var byteNumbers = new Uint8Array(byteCharacters.length);
                for (var i = 0; i < byteCharacters.length; i++) {
                    byteNumbers[i] = byteCharacters.charCodeAt(i);
                }

                console.log("byteNumbers, ", [byteNumbers]);

                // Создаем Blob из бинарных данных
                var blob = new Blob([byteNumbers], {type: 'application/pdf'}); // Укажите правильный MIME-тип

                // Создаем ссылку для скачивания
                var link = document.createElement('a');

                link.href = window.URL.createObjectURL(blob);
                link.download = "file.pdf"; // Укажите имя файла
                document.body.appendChild(link);
                link.click(); // Имитируем клик по ссылке
                document.body.removeChild(link); // Удаляем ссылку из DOM

                return byteCharacters;
            })
            .catch(e => {
                const err = getError(e);
                throw new Error(err);
            });
    } else {
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
            } catch (e) {
                const err = getError(e);
                throw new Error(err);
            }
        });
    }
};

//Подпись файла с использованием FileAPI
window.signDataAPI = function () {
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

/**
 * Получить текст ошибки
 * @param {Error} e
 * @returns {string}
 */
function getError(e) {
    console.log('Crypto-Pro error', e.message || e);
    if (e.message) {
        for (var i in cadesErrorMesages) {
            if (cadesErrorMesages.hasOwnProperty(i)) {
                if (e.message.indexOf(i) + 1) {
                    e.message = cadesErrorMesages[i];
                    break;
                }
            }
        }
    }
    return e.message || e;
}

export const cadesErrorMesages = {
    '0x800B010A': 'Не удается построить цепочку сертификатов до доверенного корневого центра, убедитесь что установлены все корневые и промежуточные сертификаты [0x800B010A]',
    '0x80090020': 'Внутренняя ошибка [0x80090020]. Если используется внешний токен, убедитесь, что ввели корректный PIN-код', // 2148073504
    '0x8007065B': 'Истекла лицензия на КриптоПро CSP [0x8007065B]',
    '0x800B0109': 'Отсутствует сертификат УЦ в хранилище корневых сертификатов [0x800B0109]', // A certificate chain processed, but terminated in a root certificate which is not trusted by the trust provider.
    '0x8009200C': 'Не удается найти сертификат и закрытый ключ для расшифровки [0x8009200C]',
    '0x80090008': 'Указан неверный алгоритм (используется устаревшая версия КриптоПро CSP или КриптоПро ЭЦП Browser plug-in) [0x80090008]', // 2148073480
    '0x000004C7': 'Операция отменена пользователем [0x000004C7]', // Не удается получить доступ к сертификатам
    '0x8009000D': 'Нет доступа к закрытому ключу. Ввод пароля отменен или произошел сбой в запомненных паролях [0x8009000D]',
    '0x800B0101': 'Истек/не наступил срок действия требуемого сертификата [0x800B0101]',
    // untested:
    '0x80070026': 'Недопустимый формат данных записываемого сертификата [0x80070026]', // @see issue #20
    '0x8009200B': 'Не удается найти закрытый ключ для подписи, убедитесь что сертификат установлен правильно [0x8009200B]',
    '0x8010006E': 'Действие отменено пользователем [0x8010006E]', // 2148532334
    'NPObject': 'Не удается подписать, убедитесь что выбранный сертификат подходит для подписи', // Error calling method on NPObject!
    'Automation server': 'Библиотека CAPICOM не была автоматически зарегистрирована или заблокирована на Вашем компьютере (2146827859)',
    'сервером программирования': 'Библиотека CAPICOM не была автоматически зарегистрирована или заблокирована на Вашем компьютере (2146827859)'
};