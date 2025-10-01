<template>
  <section>
    <div class="header-actions">
      <h2>대시보드</h2>
      <div class="user-actions">
        <span v-if="user" class="user-name">{{ user.name }}님</span>
        <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="stat-card">
        <h4>오늘 매출</h4>
        <p>₩ {{ formatNumber(dashboardSummary.todaySales) }}</p>
        <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsYesterday)]">
          {{ getComparisonText(dashboardSummary.salesChangeVsYesterday) }} vs 어제
        </span>
      </div>
      <div class="stat-card">
        <h4>이번 주 매출</h4>
        <p>₩ {{ formatNumber(dashboardSummary.thisWeekSales) }}</p>
        <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsLastWeek)]">
          {{ getComparisonText(dashboardSummary.salesChangeVsLastWeek) }} vs 지난주
        </span>
      </div>
      <div class="stat-card">
        <h4>이번 달 매출</h4>
        <p>₩ {{ formatNumber(dashboardSummary.thisMonthSales) }}</p>
        <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsLastMonth)]">
          {{ getComparisonText(dashboardSummary.salesChangeVsLastMonth) }} vs 지난달
        </span>
      </div>
    </div>

    <div class="chart-container">
      <div class="chart-header">
        <div class="chart-title-group">
          <h3>매출 분석</h3>
          <div class="chart-main-filters">
            <select v-model="chartFilters.hotelId" class="filter-select small">
              <option :value="null">모든 호텔</option>
              <option v-for="hotel in myHotels" :key="hotel.id" :value="hotel.id">{{ hotel.name }}</option>
            </select>
            <select v-model="chartFilters.roomType" class="filter-select small">
              <option :value="null">모든 객실</option>
              <option v-for="roomType in allRoomTypes" :key="roomType" :value="roomType">{{ roomType }}</option>
            </select>
            <flat-pickr
              v-model="chartFilters.dateRange"
              :config="chartDateConfig"
              placeholder="날짜 범위 선택"
              class="date-picker-placeholder small"
            />  
          </div>
        </div>
        
        <div class="chart-period-filters">
          <button class="filter-btn" :class="{ active: activePeriod === '7days' }" @click="setPeriod('7days')">최근 7일</button>
          <button class="filter-btn" :class="{ active: activePeriod === '30days' }" @click="setPeriod('30days')">최근 30일</button>
          <button class="filter-btn" :class="{ active: activePeriod === '1year' }" @click="setPeriod('1year')">최근 1년</button>
          <button class="filter-btn reset-btn" @click="clearChartFilters">초기화</button>
        </div>
      </div>
      
      <div class="chart-placeholder">
          <SalesChart v-if="chartData.length > 0" :sales-data="chartData" :time-unit="chartTimeUnit" />
          <p v-else>해당 기간에 표시할 데이터가 없습니다.</p>
      </div>
    </div>

    <div class="dashboard-grid secondary">
      <div class="info-card">
        <h4>오늘의 현황</h4>
        <div class="check-in-out-tabs">
          <button :class="{ active: activeTab === 'check-in' }" @click="activeTab = 'check-in'">체크인 ({{ todaysCheckIns.length }})</button>
          <button :class="{ active: activeTab === 'check-out' }" @click="activeTab = 'check-out'">체크아웃 ({{ todaysCheckOuts.length }})</button>
        </div>

        <ul v-if="activeTab === 'check-in'" class="guest-list">
          <li v-for="reservation in todaysCheckIns" :key="reservation.id" >
            <span>{{ reservation.guestName }}</span>
            <span class="room-type">{{ reservation.roomType }}</span>
          </li>
        </ul>
        <ul v-if="activeTab === 'check-out'" class="guest-list">
          <li v-for="reservation in todaysCheckOuts" :key="reservation.id">
            <span>{{ reservation.guestName }}</span>
            <span class="room-type">{{ reservation.roomType }}</span>
          </li>
        </ul>
      </div>

      <div class="info-card">
        <h4>최근 예약</h4>
        <ul class="activity-list">
          <li v-for="reservation in recentReservations" :key="reservation.id">
            <p><strong>{{ reservation.guestName }}</strong>님이 <strong>{{ reservation.roomType }}</strong> 예약을 완료했습니다.</p>
            <span class="time-ago">{{ formatTimeAgo(reservation.createdAt) }}</span>
          </li>
        </ul>
      </div>

      <div class="info-card">
        <h4>최근 리뷰</h4>
        <ul class="activity-list">
          <li v-for="review in recentReviews" :key="review.id" style="cursor: pointer;">
            <p><strong>{{ review.author }}</strong>님이 리뷰를 남겼습니다.</p>
            <div class="star-rating small">
              <span v-for="n in 5" :key="n" :class="{ 'filled': n <= review.star_rating }">★</span>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script>
import axios from "axios";
import flatPickr from 'vue-flatpickr-component';
import 'flatpickr/dist/flatpickr.css';
import { Korean } from "flatpickr/dist/l10n/ko.js";
import SalesChart from './SalesChart.vue'; 

export default {
  name: 'OwnerDashboard',
  components: {
    flatPickr,
    SalesChart
  },
  props: {
    user: Object
  },
  data() {
    return {
      dashboardSummary: { todaySales: 0, thisWeekSales: 0, thisMonthSales: 0, salesChangeVsYesterday: 0, salesChangeVsLastWeek: 0, salesChangeVsLastMonth: 0, },
      chartDateConfig: {
        showMonths: 2, mode: "range", dateFormat: "Y-m-d", altInput: true, altFormat: "Y년 m월 d일", locale: Korean,
        onReady: (_, __, instance) => this.updateChartCalendarHeaders(instance),
        onMonthChange: (_, __, instance) => this.$nextTick(() => this.updateChartCalendarHeaders(instance)),
      },
      chartFilters: { hotelId: null, roomType: null, dateRange: [], },
      chartData: [],
      activePeriod: '7days',
      chartTimeUnit: 'day',
      myHotels: [],
      allRoomTypes: ['스위트룸', '디럭스룸', '스탠다드룸', '싱글룸', '트윈룸'],
      allReviews: [],
      activeTab: 'check-in',
      todaysCheckIns: [],
      todaysCheckOuts: [],
      recentReservations: [],
    };
  },
  computed: {
    recentReviews() {
      return this.allReviews
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        .slice(0, 5);
    },
  },
  methods: {
    getAuthHeaders() {
      const token = localStorage.getItem('token');
      if (!token) { this.$router.push("/login"); return null; }
      return { 'Authorization': `Bearer ${token}` };
    },
    async fetchMyHotelsForFilter() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const res = await axios.get(`/api/owner/hotels/my-hotels`, { headers });
        this.myHotels = res.data;
      } catch (err) { console.error("필터용 호텔 목록 조회 실패:", err); }
    },
    async fetchDashboardSummary() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const response = await axios.get('/api/owner/dashboard/summary', { headers });
        this.dashboardSummary = response.data;
      } catch (error) { console.error("대시보드 요약 정보 조회 실패:", error); }
    },
    formatNumber(num, fractionDigits = 0) {
      if (num == null) return '0';
      return num.toLocaleString('ko-KR', { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits, });
    },
    getComparisonClass(change) {
      if (change > 0) return 'positive';
      if (change < 0) return 'negative';
      return '';
    },
    getComparisonText(change) {
      if (change === 0 || !isFinite(change)) return '-';
      const sign = change > 0 ? '+' : '';
      return `${sign}${this.formatNumber(change, 1)}%`;
    },
    clearChartFilters() {
      this.chartFilters.hotelId = null; this.chartFilters.roomType = null; this.chartFilters.dateRange = []; 
      this.setPeriod('7days');
    },
    updateChartCalendarHeaders(instance) {
      if (!instance.calendarContainer) return;
      instance.calendarContainer.querySelectorAll('.numInputWrapper, .arrowUp, .arrowDown').forEach(el => { el.style.display = 'none'; });
      instance.calendarContainer.querySelectorAll('.flatpickr-current-month').forEach((header, index) => {
        header.innerHTML = '';
        const now = new Date();
        const baseMonth = (instance.currentMonth ?? now.getMonth()) + index;
        const baseYear  = (instance.currentYear  ?? now.getFullYear());
        const displayYear  = baseYear + Math.floor(baseMonth / 12);
        const displayMonth = ((baseMonth % 12) + 12) % 12;
        const monthNames = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'];
        const textSpan = document.createElement('span');
        textSpan.textContent = `${displayYear}년 ${monthNames[displayMonth]}`;
        textSpan.style.cssText = 'font-size:16px; font-weight:600; color:#333;';
        header.appendChild(textSpan);
      });
    },
    async fetchDailyChartData() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      let startDate, endDate;
      const dateRange = this.chartFilters.dateRange;
      if (typeof dateRange === 'string' && dateRange.includes(' to ')) {
        const dates = dateRange.split(' to ');
        startDate = new Date(dates[0]);
        endDate = new Date(dates[1]);
      } else {
        const today = new Date(); today.setHours(0, 0, 0, 0); endDate = new Date(today); startDate = new Date(today);
        if (this.activePeriod === '7days') { startDate.setDate(today.getDate() - 6); } 
        else if (this.activePeriod === '30days') { startDate.setDate(today.getDate() - 29); }
      }
      const formatDate = (date) => date.toISOString().split('T')[0];
      const requestBody = { startDate: formatDate(startDate), endDate: formatDate(endDate), hotelId: this.chartFilters.hotelId, roomType: this.chartFilters.roomType, };
      try {
        const response = await axios.post('/api/owner/dashboard/daily-sales', requestBody, { headers });
        this.chartData = this.fillMissingDates(response.data, startDate, endDate);
      } catch (error) { console.error("일별 차트 데이터 조회 실패:", error); this.chartData = []; }
    },
    async fetchMonthlyChartData() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      const endDate = new Date(); const startDate = new Date(); startDate.setFullYear(endDate.getFullYear() - 1);
      const formatDate = (date) => date.toISOString().split('T')[0];
      const requestBody = { startDate: formatDate(startDate), endDate: formatDate(endDate), hotelId: this.chartFilters.hotelId, roomType: this.chartFilters.roomType, };
      try {
        const response = await axios.post('/api/owner/dashboard/monthly-sales', requestBody, { headers });
        this.chartData = this.fillMissingMonths(response.data, startDate, endDate);
      } catch (error) { console.error("월별 차트 데이터 조회 실패:", error); this.chartData = []; }
    },
    fillMissingMonths(data, startDate, endDate) {
      const salesMap = new Map(data.map(item => [item.yearMonth, item.totalSales]));
      const filledData = []; let currentDate = new Date(startDate.getFullYear(), startDate.getMonth(), 1);
      while (currentDate <= endDate) {
        const year = currentDate.getFullYear(); const month = String(currentDate.getMonth() + 1).padStart(2, '0'); const yearMonth = `${year}-${month}`;
        filledData.push({ date: yearMonth, totalSales: salesMap.get(yearMonth) || 0 });
        currentDate.setMonth(currentDate.getMonth() + 1);
      }
      return filledData;
    },
    fillMissingDates(data, startDate, endDate) {
      const salesMap = new Map(data.map(item => [item.date, item.totalSales]));
      const filledData = []; let currentDate = new Date(startDate);
      while (currentDate <= endDate) {
        const dateString = currentDate.toISOString().split('T')[0];
        filledData.push({ date: dateString, totalSales: salesMap.get(dateString) || 0 });
        currentDate.setDate(currentDate.getDate() + 1);
      }
      return filledData;
    },
    setPeriod(period) {
      this.activePeriod = period;
      if (this.flatpickrInstance) { this.flatpickrInstance.clear(); }
      this.chartFilters.dateRange = [];
      this.updateChartData();
    },
    updateChartData() {
      if (this.activePeriod === '1year') { this.chartTimeUnit = 'month'; this.fetchMonthlyChartData(); } 
      else { this.chartTimeUnit = 'day'; this.fetchDailyChartData(); }
    },
    async fetchDashboardActivity() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const response = await axios.get('/api/owner/dashboard/activity', { headers });
        const data = response.data;
        this.todaysCheckIns = data.checkIns; this.todaysCheckOuts = data.checkOuts; this.recentReservations = data.recentReservations;
      } catch (error) { console.error("대시보드 활동 정보 조회 실패:", error); }
    },
    formatTimeAgo(dateString) {
      if (!dateString) return '';
      const now = new Date(); const past = new Date(dateString); const diffInSeconds = Math.floor((now - past) / 1000);
      const minutes = Math.floor(diffInSeconds / 60); if (minutes < 1) return '방금 전'; if (minutes < 60) return `${minutes}분 전`;
      const hours = Math.floor(minutes / 60); if (hours < 24) return `${hours}시간 전`;
      const days = Math.floor(hours / 24); return `${days}일 전`;
    },
    async fetchRecentReviews() {
        const headers = this.getAuthHeaders();
        if (!headers) return;
        try {
            const response = await axios.get('/api/owner/dashboard/recent-reviews', { headers });
            this.allReviews = response.data.map(review => ({ id: review.id, author: review.authorName, hotelName: review.hotelName, star_rating: review.rating, createdAt: review.createdAt, }));
        } catch (error) { console.error("최근 리뷰 조회 실패:", error); }
    },
  },
  watch: {
    chartFilters: {
      handler() {
        const dateRange = this.chartFilters.dateRange;
        if (typeof dateRange === 'string' && dateRange.includes(' to ')) { this.activePeriod = 'custom'; }
        this.updateChartData();
      },
      deep: true
    },
  },
  mounted() {
    this.fetchMyHotelsForFilter();
    this.fetchDashboardSummary();
    this.fetchDailyChartData();
    this.fetchDashboardActivity();
    this.fetchRecentReviews();
  }
}
</script>

<style scoped>
section { padding: 30px; }
h2 { margin: 0; font-size: 24px; color: #111827; }
h3 { margin-top: 20px; font-size: 20px; color: #111827; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.user-actions { display: flex; align-items: center; gap: 15px; }
.user-name { font-weight: 600; color: #374151; }
.logout-btn { padding: 10px 16px; background: #6b7280; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
.logout-btn:hover { background: #4b5563; }
.dashboard-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 25px; margin-bottom: 30px; }
.stat-card { background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.stat-card h4 { margin: 0 0 10px; font-size: 16px; color: #6b7280; }
.stat-card p { margin: 0 0 10px; font-size: 28px; font-weight: 700; color: #111827; }
.stat-card .comparison { font-size: 14px; }
.comparison.positive { color: #10b981; }
.comparison.negative { color: #ef4444; }
.chart-container { background: #fff; padding: 25px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); margin-bottom: 30px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 20px; }
.chart-title-group { display: flex; align-items: center; gap: 15px; flex-grow: 1; }
.chart-header h3 { margin: 0; font-size: 18px; white-space: nowrap; }
.chart-main-filters { display: flex; align-items: center; gap: 10px; }
.chart-period-filters { display: flex; gap: 10px; flex-shrink: 0; }
.filter-select.small { padding: 8px 12px; font-size: 14px; height: 38px; border: 1px solid #d1d5db; border-radius: 6px; background-color: #fff; }
:deep(.date-picker-placeholder.small) { padding: 8px 12px; font-size: 14px; height: 38px; border: 1px solid #d1d5db; border-radius: 6px; background-color: #fff; min-width: 260px; }
.filter-btn { background: #f3f4f6; border: 1px solid #e5e7eb; padding: 8px 14px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.filter-btn.active, .filter-btn:hover { background: #3b82f6; color: white; border-color: #3b82f6; }
.filter-btn.reset-btn { background: #6b7280; color: white; border-color: #6b7280; }
.filter-btn.reset-btn:hover { background: #4b5563; border-color: #4b5563; }
.chart-placeholder { background: #f9fafb; border-radius: 8px; height: 300px; display: flex; align-items: center; justify-content: center; color: #9ca3af; font-size: 16px; }
.dashboard-grid.secondary { grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); }
.info-card { background: #fff; padding: 25px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.info-card h4 { margin: 0 0 20px; padding-bottom: 15px; border-bottom: 1px solid #e5e7eb; font-size: 16px; }
.check-in-out-tabs { display: flex; margin-bottom: 15px; border-radius: 8px; background: #f3f4f6; padding: 5px; }
.check-in-out-tabs button { flex: 1; padding: 8px; border: none; background: transparent; border-radius: 6px; cursor: pointer; font-weight: 600; transition: background-color .2s, color .2s; }
.check-in-out-tabs button.active { background: #fff; color: #3b82f6; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.guest-list, .activity-list { list-style: none; padding: 0; margin: 0; }
.guest-list { max-height: 225px; overflow-y: auto; }
.activity-list { max-height: 280px; overflow-y: auto; }
.guest-list li, .activity-list li { display: flex; justify-content: space-between; padding: 10px 5px; border-bottom: 1px solid #f3f4f6; }
.guest-list li:last-child, .activity-list li:last-child { border-bottom: none; }
.guest-list .room-type { color: #6b7280; font-size: 14px; }
.activity-list { display: flex; flex-direction: column; gap: 15px; }
.activity-list li { flex-direction: column; align-items: flex-start; padding: 0; border: none; }
.activity-list p { margin: 0 0 5px; font-size: 14px; }
.activity-list .time-ago, .activity-list .star-rating { font-size: 12px; color: #9ca3af; }
.star-rating span { color: #d1d5db; }
.star-rating span.filled { color: #f59e0b; }
.star-rating.small span { font-size: 14px; }
.star-rating.small span.filled { color: #f59e0b; }
.star-rating.small span:not(.filled) { color: #d1d5db; }
</style>    