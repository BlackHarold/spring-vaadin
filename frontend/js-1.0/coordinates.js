// var x, y;
// // Создаем функцию для обработки клика
// document.addEventListener('click', function (event) {
//     console.log("event click!!!");
//     // Получаем координаты клика
//     x = event.clientX;
//     y = event.clientY;
//     console.log("x ", x, " y ", y);
//     this.$server.setCoordinates(x, y);
//
//     // Выводим координаты в виде уведомления
//     alert("Координаты клика: X = " + x + ", Y = " + y);
//
//     // Или выводим координаты на экран
//     var coordinatesDisplay = document.getElementById('coordinatesDisplay');
//     if (coordinatesDisplay) {
//         coordinatesDisplay.innerText = "Координаты клика: X = " + x + ", Y = " + y;
//     }
// });

window.setCoordinates = function () {
    // Отправляем координаты обратно в Java
    // Получаем координаты клика
    x = event.clientX;
    y = event.clientY;
    console.log("x ", x, " y ", y);
};