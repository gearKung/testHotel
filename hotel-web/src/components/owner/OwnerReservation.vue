<template>
  <section class="reservations-section compact">
    <div class="header-actions">
      <h2>예약 관리</h2>
      <div class="user-actions">
        <span v-if="user" class="user-name">{{ user.name }}님</span>
        <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
      </div>
    </div>

    <div class="top-filter-container">
        <select id="hotel-filter" v-model="filterHotel" class="filter-select">
            <option value="ALL">모든 호텔</option>
            <option v-for="hotel in myHotels" :key="hotel.id" :value="hotel.name">{{ hotel.name }}</option>
        </select>
        <select id="room-type-filter" v-model="filterRoomType" class="filter-select">
            <option value="ALL">모든 객실</option>
            <option v-for="roomType in allRoomTypes" :key="roomType" :value="roomType">{{ roomType }}</option>
        </select>
    </div>

    <div class="reservations-content-compact">
      <div class="calendar-container">
        <FullCalendar ref="fullCalendar" :options="calendarOptions" />
      </div>
      <div class="reservation-sidebar">
        <div class="sidebar-header">
          <h3>{{ selectedDate ? `${selectedDate} 예약` : '최근 예약' }}</h3>
          <button v-if="selectedDate" @click="clearDateFilter" class="btn-clear-filter">초기화</button>
        </div>
        <div class="list-controls">
          <input type="text" v-model="searchKeyword" placeholder="예약자명 검색" class="search-input"/>
          <select v-model="filterStatus" class="filter-select">
              <option value="ALL">모든 상태</option>
              <option value="COMPLETED">예약 완료</option>
              <option value="CANCELLED">예약 취소</option>
          </select>
          <div class="date-filter-tabs">
            <button :class="{ active: filterDateType === 'all' }" @click="setFilterDateType('all')">전체</button>
            <button :class="{ active: filterDateType === 'check-in' }" @click="setFilterDateType('check-in')">체크인</button>
            <button :class="{ active: filterDateType === 'check-out' }" @click="setFilterDateType('check-out')">체크아웃</button>
          </div>
        </div>
        <ul class="reservation-list">
          <li v-for="reservation in filteredReservations" :key="reservation.id" class="reservation-card" @click="showReservationDetails(reservation)">
            <div class="card-header">
              <strong>{{ reservation.guestName }}</strong>
              <span :class="`status-badge ${reservation.status.toLowerCase()}`">{{ reservation.statusLabel }}</span>
            </div>
            <div class="card-body">
              <p>{{ reservation.roomType }}</p>
              <p>{{ reservation.checkInDate }} ~ {{ reservation.checkOutDate }}</p>
            </div>
          </li>
          <li v-if="filteredReservations.length === 0" class="no-reservations">
            해당 예약이 없습니다.
          </li>
        </ul>
      </div>
    </div>
    
    <div v-if="selectedReservation" class="modal-overlay" @click.self="closeReservationDetails">
        <div class="modal-content">
          <button class="modal-close-btn" @click="closeReservationDetails">✕</button>
          <h3>예약 상세 정보</h3>
          <div class="modal-grid">
            <div class="modal-item"><strong>예약 번호:</strong><span>{{ selectedReservation.id }}</span></div>
            <div class="modal-item"><strong>예약 상태:</strong><span :class="`status-badge ${selectedReservation.status.toLowerCase()}`">{{ selectedReservation.statusLabel }}</span></div>
            <div class="modal-item"><strong>예약자명:</strong><span>{{ selectedReservation.guestName }}</span></div>
            <div class="modal-item"><strong>연락처:</strong><span>{{ selectedReservation.guestPhone }}</span></div>
            <div class="modal-item"><strong>호텔:</strong><span>{{ selectedReservation.hotelName }}</span></div>
            <div class="modal-item"><strong>객실 타입:</strong><span>{{ selectedReservation.roomType }}</span></div>
            <div class="modal-item full-width"><strong>체크인/아웃:</strong><span>{{ selectedReservation.checkInDate }} ~ {{ selectedReservation.checkOutDate }} ({{ selectedReservation.nights }}박)</span></div>
            <div class="modal-item"><strong>성인:</strong><span>{{ selectedReservation.adults }}명</span></div>
            <div class="modal-item"><strong>어린이:</strong><span>{{ selectedReservation.children }}명</span></div>
            <div class="modal-item full-width"><strong>요청사항:</strong><span>{{ selectedReservation.requests || '없음' }}</span></div>
          </div>
          <div class="modal-actions">
            <div class="check-in-out-actions">
              <button v-if="shouldShowCheckInButton(selectedReservation)" @click="toggleCheckIn(selectedReservation)" :class="getCheckInButtonClass(selectedReservation)">
                {{ getCheckInButtonText(selectedReservation) }}
              </button>
              <button v-if="shouldShowCheckOutButton(selectedReservation)" @click="toggleCheckOut(selectedReservation)" :class="getCheckOutButtonClass(selectedReservation)">
                {{ getCheckOutButtonText(selectedReservation) }}
              </button>
              <button class="btn-danger" @click="cancelReservation(selectedReservation.id)" :disabled="!isCancellable(selectedReservation)" :class="{ 'disabled': !isCancellable(selectedReservation) }">
                예약 취소
              </button>
            </div>
          </div>
        </div>
      </div>
  </section>
</template>

<script>
import axios from "axios";
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';

export default {
  name: 'OwnerReservation',
  components: { FullCalendar },
  props: { user: Object },
  data() {
    return {
      myHotels: [], allReservations: [], selectedReservation: null, selectedDate: null,
      searchKeyword: '', filterStatus: 'COMPLETED', filterHotel: 'ALL', filterRoomType: 'ALL',
      allRoomTypes: ['스위트룸', '디럭스룸', '스탠다드룸', '싱글룸', '트윈룸'],
      isWheelScrolling: false, wheelScrollTimer: null,
      calendarOptions: {
        plugins: [dayGridPlugin, interactionPlugin], initialView: 'dayGridMonth',
        headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,dayGridWeek' },
        locale: 'ko', events: [], dateClick: this.handleDateClick, eventClick: this.handleEventClick,
        dayMaxEvents: 3, views: { dayGridWeek: { dayMaxEvents: 10 } },
      },
      checkInStatus: {}, filterDateType: 'all', 
    };
  },
  computed: {
    filteredReservations() {
      let reservations = this.allReservations; const today = new Date().toISOString().split('T')[0];
      if (this.filterDateType === 'check-in') { reservations = reservations.filter(r => r.checkInDate === today); } 
      else if (this.filterDateType === 'check-out') { reservations = reservations.filter(r => r.checkOutDate === today); }
      else if (this.selectedDate) {
        const selected = new Date(this.selectedDate); selected.setHours(0, 0, 0, 0);
        reservations = reservations.filter(r => {
          const checkIn = new Date(r.checkInDate); checkIn.setHours(0, 0, 0, 0);
          const checkOut = new Date(r.checkOutDate); checkOut.setHours(0, 0, 0, 0);
          return checkIn <= selected && selected < checkOut;
        });
      }
      if (this.filterStatus !== 'ALL') { reservations = reservations.filter(r => r.status === this.filterStatus); }
      if (this.filterHotel !== 'ALL') { reservations = reservations.filter(r => r.hotelName === this.filterHotel); }
      if (this.filterRoomType !== 'ALL') { reservations = reservations.filter(r => r.roomType === this.filterRoomType); }
      if (this.searchKeyword.trim() !== '') {
        const keyword = this.searchKeyword.toLowerCase();
        reservations = reservations.filter(r => r.guestName.toLowerCase().includes(keyword));
      }
      return reservations;
    },
    isCancellable() {
      return (reservation) => {
        if (!reservation || reservation.status === 'CANCELLED') return false;
        const today = new Date(); today.setHours(0, 0, 0, 0); 
        const checkInDate = new Date(reservation.checkInDate); checkInDate.setHours(0, 0, 0, 0);
        return checkInDate > today;
      };
    },
    filteredCalendarEvents() {
      const roomTypeColors = { '스위트룸': '#FFD700', '디럭스룸': '#87CEEB', '스탠다드룸': '#32CD32', '싱글룸': '#FFA07A', '트윈룸': '#B19CD9' };
      return this.filteredReservations.map(r => {
        const endDate = new Date(r.checkOutDate);
        const eventColor = r.status === 'COMPLETED' ? (roomTypeColors[r.roomType] || '#10b981') : '#6b7280';
        return {
          title: `${r.guestName} (${r.roomType})`,
          start: r.checkInDate,
          end: r.checkOutDate,
          color: eventColor,
          extendedProps: { reservation: r }
        };
      });
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
    async fetchReservations() {
      if (!this.user) return;
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        const response = await axios.get(`/api/owner/reservations`, { headers });
        this.allReservations = response.data.filter(r => r.status !== 'PENDING');
      } catch (error) { console.error("예약 조회 실패:", error); alert("예약 정보를 불러오는 데 실패했습니다."); }
    },
    handleDateClick(arg) { this.selectedDate = arg.dateStr; this.filterDateType = 'all'; },
    setFilterDateType(type) { this.filterDateType = type; this.selectedDate = null; },
    handleEventClick(clickInfo) {
      if (clickInfo.event.extendedProps.reservation) { this.showReservationDetails(clickInfo.event.extendedProps.reservation); }
      const popover = document.querySelector('.fc-popover');
      if (popover) { popover.style.display = 'none'; }
    },
    showReservationDetails(reservation) { this.selectedReservation = reservation; },
    closeReservationDetails() { this.selectedReservation = null; },
    handleWheelScroll(event) {
      event.preventDefault();
      if (this.isWheelScrolling) return;
      this.isWheelScrolling = true;
      if (this.$refs.fullCalendar) {
        const calendarApi = this.$refs.fullCalendar.getApi();
        if (event.deltaY < 0) { calendarApi.prev(); } else { calendarApi.next(); }
      }
      this.wheelScrollTimer = setTimeout(() => { this.isWheelScrolling = false }, 300);
    },
    clearDateFilter() { this.selectedDate = null; },
    async cancelReservation(reservationId) {
      if (!confirm("정말로 이 예약을 취소하시겠습니까?")) return;
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.post(`/api/owner/reservations/${reservationId}/cancel`, {}, { headers });
        alert("예약이 성공적으로 취소되었습니다.");
        this.closeReservationDetails(); await this.fetchReservations();
      } catch (error) { console.error("예약 취소 실패:", error); alert("예약 취소 중 오류가 발생했습니다."); }
    },
    isToday(dateStr) {
      if (!dateStr) return false;
      return dateStr === new Date().toISOString().split('T')[0];
    },
    shouldShowCheckInButton(r) { return this.isToday(r.checkInDate) && r.status === 'COMPLETED'; },
    shouldShowCheckOutButton(r) { return this.checkInStatus[r.id] === 'CHECKED_IN' && this.isToday(r.checkOutDate) && r.status === 'COMPLETED'; },
    getCheckInButtonText(r) { return this.checkInStatus[r.id] === 'CHECKED_IN' ? '체크인 취소' : '체크인'; },
    getCheckOutButtonText(r) { return this.checkInStatus[r.id] === 'CHECKED_OUT' ? '체크아웃 취소' : '체크아웃'; },
    getCheckInButtonClass(r) { return this.checkInStatus[r.id] === 'CHECKED_IN' ? 'btn-cancel' : 'btn-confirm'; },
    getCheckOutButtonClass(r) { return this.checkInStatus[r.id] === 'CHECKED_OUT' ? 'btn-cancel' : 'btn-confirm'; },
    toggleCheckIn(r) {
      if (this.checkInStatus[r.id] === 'CHECKED_IN') { delete this.checkInStatus[r.id]; } 
      else { this.checkInStatus[r.id] = 'CHECKED_IN'; }
    },
    toggleCheckOut(r) {
      if (this.checkInStatus[r.id] === 'CHECKED_OUT') { this.checkInStatus[r.id] = 'CHECKED_IN'; } 
      else { this.checkInStatus[r.id] = 'CHECKED_OUT'; }
    },
  },
  watch: {
    filteredCalendarEvents: { handler(newEvents) { this.calendarOptions.events = newEvents; }, immediate: true },
  },
  mounted() {
    this.fetchMyHotelsForFilter();
    this.fetchReservations();
    this.$nextTick(() => {
      const calendarEl = this.$refs.fullCalendar?.$el;
      if (calendarEl) { calendarEl.addEventListener('wheel', this.handleWheelScroll, { passive: false }); }
    });
  },
  beforeUnmount() {
    clearTimeout(this.wheelScrollTimer);
    const calendarEl = this.$refs.fullCalendar?.$el;
    if (calendarEl) { calendarEl.removeEventListener('wheel', this.handleWheelScroll); }
  }
}
</script>

<style scoped>
.reservations-section.compact { display: flex; flex-direction: column; height: calc(100vh - 60px); padding: 30px 15px; box-sizing: border-box; }
.header-actions { padding: 0 15px; }
h2 { margin: 0; font-size: 24px; color: #111827; }
.user-actions { display: flex; align-items: center; gap: 15px; }
.user-name { font-weight: 600; color: #374151; }
.logout-btn { padding: 10px 16px; background: #6b7280; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
.logout-btn:hover { background: #4b5563; }
.top-filter-container { display: flex; align-items: center; gap: 15px; margin: 0 15px 20px; background-color: #fff; padding: 15px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.top-filter-container .filter-select { width: auto; min-width: 150px; }
.reservations-content-compact { display: flex; gap: 15px; flex-grow: 1; overflow: hidden; height: 100%; padding: 0 15px; }
.calendar-container { flex: 1; min-width: 0; background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); display: flex; flex-direction: column; }
.calendar-container :deep(.fc) { height: 100%; }
.calendar-container :deep(.fc-header-toolbar) { display: flex; justify-content: space-between; flex-wrap: wrap; }
.reservation-sidebar { width: 320px; flex-shrink: 0; background: #fff; padding: 20px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); display: flex; flex-direction: column; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; margin: 0 0 20px; }
.sidebar-header h3 { margin: 0; font-size: 18px; }
.btn-clear-filter { background: #e5e7eb; color: #374151; border: none; padding: 8px 14px; font-size: 12px; font-weight: 700; border-radius: 6px; cursor: pointer; transition: background-color .2s; }
.btn-clear-filter:hover { background: #d1d5db; }
.list-controls { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.search-input, .filter-select { width: 100%; padding: 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; }
.date-filter-tabs { display: flex; gap: 8px; background-color: #e5e7eb; padding: 4px; border-radius: 8px; }
.date-filter-tabs button { flex: 1; padding: 4px 8px; border: none; background-color: transparent; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500; color: #4b5563; }
.date-filter-tabs button.active { background-color: #fff; color: #111827; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.reservation-list { list-style: none; padding: 0; margin: 0; overflow-y: auto; flex-grow: 1; } 
.reservation-card { background: #f9fafb; padding: 15px; border-radius: 8px; margin-bottom: 10px; cursor: pointer; transition: background-color .2s, box-shadow .2s; border: 1px solid #e5e7eb; }
.reservation-card:hover { background-color: #f3f4f6; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.card-header strong { font-size: 16px; }
.status-badge { padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: 700; color: white; }
.status-badge.completed { background-color: #10b981; }
.status-badge.cancelled { background-color: #6b7280; }
.card-body p { margin: 0; font-size: 14px; color: #4b5563; }
.no-reservations { text-align: center; padding: 40px; color: #9ca3af; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.6); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-content { background: white; padding: 30px; border-radius: 12px; width: 90%; max-width: 600px; position: relative; }
.modal-close-btn { position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 24px; cursor: pointer; color: #6b7280; }
.modal-content h3 { margin: 0 0 20px; }
.modal-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.modal-item { display: flex; flex-direction: column; background-color: #f9fafb; padding: 10px; border-radius: 6px; }
.modal-item strong { font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.modal-item span { font-size: 15px; }
.modal-item.full-width { grid-column: 1 / -1; }
.modal-actions { margin-top: 20px; padding-top: 20px; border-top: 1px solid #e5e7eb; display: flex; justify-content: flex-end; align-items: center; }
.check-in-out-actions { display: flex; gap: 10px; margin-right: auto; }
.check-in-out-actions button { padding: 10px 16px; border: none; border-radius: 6px; font-weight: 700; cursor: pointer; }
.btn-confirm { background-color: #10b981; color: white; }
.btn-cancel { background-color: #f59e0b; color: white; }
.btn-danger { background-color: #ef4444; color: #fff; padding: 10px 16px; border: none; border-radius: 6px; font-weight: 700; cursor: pointer; }
.btn-danger.disabled { background-color: #9ca3af; cursor: not-allowed; }
</style>