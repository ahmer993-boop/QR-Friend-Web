/**
 * charts.js - Chart.js setup and state management for QR Friend Web Monitoring Dashboard
 * Library: Chart.js v4+ (UMD)
 */

let leaderboardChartInstance = null;
let statusDoughnutChartInstance = null;
let trendsLineChartInstance = null;

/**
 * Initializes all 3 visual charts with sleek styling, rounded corners, and tooltips.
 */
function initCharts() {
  // 1. BDO Performance Leaderboard (Bar Chart)
  const ctxLeaderboard = document.getElementById('bdoLeaderboardChart')?.getContext('2d');
  if (ctxLeaderboard) {
    leaderboardChartInstance = new Chart(ctxLeaderboard, {
      type: 'bar',
      data: {
        labels: [],
        datasets: [{
          label: 'Merchants Onboarded',
          data: [],
          backgroundColor: '#6366f1', // Indigo 500
          hoverBackgroundColor: '#4f46e5', // Indigo 600
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 36,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            titleFont: { size: 12, family: 'Inter' },
            bodyFont: { size: 12, family: 'Inter' },
            padding: 10,
            cornerRadius: 8,
            displayColors: false,
            callbacks: {
              label: (context) => ` ${context.parsed.y} Merchants Onboarded`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              font: { size: 11, family: 'Inter' },
              color: '#64748b',
              maxRotation: 45,
              minRotation: 0,
            }
          },
          y: {
            beginAtZero: true,
            grid: { color: '#f1f5f9' },
            ticks: {
              font: { size: 11, family: 'Inter' },
              color: '#64748b',
              precision: 0
            }
          }
        }
      }
    });
  }

  // 2. Merchant Status Breakdown (Doughnut Chart)
  const ctxStatus = document.getElementById('merchantStatusChart')?.getContext('2d');
  if (ctxStatus) {
    statusDoughnutChartInstance = new Chart(ctxStatus, {
      type: 'doughnut',
      data: {
        labels: ['Active', 'Pending', 'Rejected'],
        datasets: [{
          data: [0, 0, 0],
          backgroundColor: [
            '#10b981', // Emerald 500 (Active)
            '#f59e0b', // Amber 500 (Pending)
            '#ef4444', // Red 500 (Rejected)
          ],
          borderColor: '#ffffff',
          borderWidth: 3,
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '72%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              font: { size: 11, family: 'Inter', weight: 500 },
              color: '#475569',
              padding: 16,
              usePointStyle: true,
              pointStyle: 'circle'
            }
          },
          tooltip: {
            backgroundColor: '#0f172a',
            padding: 10,
            cornerRadius: 8,
            callbacks: {
              label: (context) => ` ${context.label}: ${context.parsed} (${context.dataset.percentages?.[context.dataIndex] || ''}%)`
            }
          }
        }
      }
    });
  }

  // 3. Onboarding Trends Over Time (Line Chart)
  const ctxTrends = document.getElementById('onboardingTrendsChart')?.getContext('2d');
  if (ctxTrends) {
    trendsLineChartInstance = new Chart(ctxTrends, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{
          label: 'New Onboardings',
          data: [],
          borderColor: '#10b981', // Emerald 500
          backgroundColor: 'rgba(16, 185, 129, 0.08)',
          borderWidth: 2.5,
          tension: 0.35,
          fill: true,
          pointBackgroundColor: '#10b981',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            padding: 10,
            cornerRadius: 8,
            callbacks: {
              label: (context) => ` ${context.parsed.y} Merchants registered on this day`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: {
              font: { size: 11, family: 'Inter' },
              color: '#64748b'
            }
          },
          y: {
            beginAtZero: true,
            grid: { color: '#f1f5f9' },
            ticks: {
              font: { size: 11, family: 'Inter' },
              color: '#64748b',
              precision: 0
            }
          }
        }
      }
    });
  }
}

/**
 * Updates the BDO Leaderboard Bar Chart with real-time aggregated data.
 * @param {Array<{name: string, count: number}>} bdoStats
 */
function updateLeaderboardChart(bdoStats) {
  if (!leaderboardChartInstance) return;
  // Sort top 10 agents
  const sorted = [...bdoStats].sort((a, b) => b.count - a.count).slice(0, 10);
  leaderboardChartInstance.data.labels = sorted.map(s => s.name);
  leaderboardChartInstance.data.datasets[0].data = sorted.map(s => s.count);
  leaderboardChartInstance.update();
}

/**
 * Updates the Status Breakdown Doughnut Chart.
 * @param {{active: number, pending: number, rejected: number}} statusCounts
 */
function updateStatusDoughnut(statusCounts) {
  if (!statusDoughnutChartInstance) return;
  const total = (statusCounts.active || 0) + (statusCounts.pending || 0) + (statusCounts.rejected || 0);
  const activePct = total ? Math.round((statusCounts.active / total) * 100) : 0;
  const pendingPct = total ? Math.round((statusCounts.pending / total) * 100) : 0;
  const rejectedPct = total ? Math.round((statusCounts.rejected / total) * 100) : 0;

  statusDoughnutChartInstance.data.datasets[0].data = [
    statusCounts.active || 0,
    statusCounts.pending || 0,
    statusCounts.rejected || 0
  ];
  statusDoughnutChartInstance.data.datasets[0].percentages = [activePct, pendingPct, rejectedPct];
  statusDoughnutChartInstance.update();

  const labelEl = document.getElementById('totalMerchantsDoughnutLabel');
  if (labelEl) labelEl.textContent = `${total} Total`;
}

/**
 * Updates the Onboarding Trends Line Chart over chronological dates.
 * @param {Array<{dateStr: string, count: number}>} timelineData
 */
function updateTrendsChart(timelineData) {
  if (!trendsLineChartInstance) return;
  trendsLineChartInstance.data.labels = timelineData.map(d => d.dateStr);
  trendsLineChartInstance.data.datasets[0].data = timelineData.map(d => d.count);
  trendsLineChartInstance.update();
}

// Attach to window object for global availability
window.DashboardCharts = {
  initCharts,
  updateLeaderboardChart,
  updateStatusDoughnut,
  updateTrendsChart
};
