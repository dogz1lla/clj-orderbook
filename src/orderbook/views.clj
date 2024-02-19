(ns orderbook.views)


(defn chartjs-init []
  [:script {:src "https://cdn.jsdelivr.net/npm/chart.js"}])

(defn chart-script
  []
  [:script "const ctx = document.getElementById('orderbook');

  function addData(chart, labels, bids, asks) {
    chart.data.datasets[0].data = bids;
    chart.data.datasets[1].data = asks;
    chart.update();
  }

  //Chart.defaults.backgroundColor = '#9BD0F5';
  Chart.defaults.borderColor = '#5D6D7E';
  Chart.defaults.color = '#FFF';

  var myChart = new Chart(ctx, {
    type: 'bar',
    data: {
      //labels: [],
      datasets: [
      {
        label: 'bid',
        data: [],
        barThickness: 1
      },
      {
        label: 'ask',
        data: [],
        barThickness: 1
      },
      ]
    },
    options: {
      animation: false,
      scales: {
        xAxis: {
          type: 'linear',
          max: 120.0,
          min: 80.0,
        },
        y: {
          beginAtZero: true
        }
      }
    }
  });

  const socket = new WebSocket('ws://localhost:3000/ws/connect')
  socket.onmessage = (event) => {
    const msg = JSON.parse(event.data)
    //console.log(msg);
    addData(myChart, msg.labels, msg.bid, msg.ask);
  };
  "])

(defn chart-element
  []
  [:div [:canvas {:id "orderbook" :style {:background-color "#212F3D"}}]])

(defn chart-view
  []
  [:body 
   (chartjs-init)
   (chart-element)
   (chart-script)])
