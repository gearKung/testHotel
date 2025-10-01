<template>
  <div>
    <section v-if="currentView === 'list'">
      <div class="header-actions">
        <h2>내 호텔 목록</h2>
        <div class="user-actions">
          <span v-if="user" class="user-name">{{ user.name }}님</span>
          <button class="add-btn" @click="openCreateForm">호텔 등록</button>
          <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
        </div>
      </div>
      <div class="hotel-grid">
        <div v-for="h in myHotels" :key="h.id" class="hotel-card" @click="showHotelDetails(h)">
          <img :src="h.imageUrls && h.imageUrls.length > 0 ? h.imageUrls[0] : 'https://via.placeholder.com/300'" alt="호텔 대표 이미지" class="hotel-card-image"/>
          <div class="hotel-card-info">
            <strong>{{ h.name }}</strong>
            <span>{{ h.address }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="currentView === 'details' && selectedHotel">
        <div class="header-actions">
            <button class="back-btn" @click="goToList">← 호텔 목록으로</button>
            <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
        </div>
        </div>
        <div class="hotel-details-view">
            <img :src="selectedHotel.imageUrls && selectedHotel.imageUrls.length > 0 ? selectedHotel.imageUrls[0] : 'https://via.placeholder.com/400'" alt="호텔 대표 이미지" class="details-image"/>
            <div class="details-info">
            <h2>{{ selectedHotel.name }}</h2>
            <p><strong>주소:</strong> {{ selectedHotel.address }}</p>
            <p><strong>나라:</strong> {{ selectedHotel.country }}</p>
            <p><strong>성급:</strong> {{ selectedHotel.starRating }}성⭐</p>
            <div class="details-actions">
                <button class="btn-edit" @click="editHotel(selectedHotel)">수정</button>
                <button class="btn-delete" @click="deleteHotel(selectedHotel.id)">삭제</button>
                <button class="btn-rooms" @click="showRoomList(selectedHotel)">객실 보기</button>
            </div>
            </div>
        </div>
    </section>
    
    <section v-if="currentView === 'rooms' && selectedHotel">
      <div class="header-actions">
        <button class="back-btn" @click="showHotelDetails(selectedHotel)">← 호텔 정보로</button>
        <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
        </div>
      </div>
      <h3>{{ selectedHotel.name }} - 객실 관리</h3>
        <div class="header-actions secondary">
        <p>등록된 객실 수: {{ rooms.length }}</p>
        <button class="add-btn" @click="openRoomCreateForm">객실 추가</button>
      </div>
      
      <ul class="room-list">
          <li v-for="room in rooms" :key="room.id" class="room-item">
            <img :src="room.imageUrls && room.imageUrls.length > 0 ? room.imageUrls[0] : 'https://via.placeholder.com/150'" alt="객실 대표 이미지" class="room-image" />
            <div class="room-info">
              <strong>{{ room.roomType }}</strong>
              <span>- 크기: {{ room.roomSize }}</span>
              <span>- 인원: {{ room.capacityMin }}~{{ room.capacityMax }}명</span>
              <span>- 가격: {{ room.price.toLocaleString() }}원</span>
            </div>
            <div class="actions">
              <button @click="editRoom(room)">수정</button>
              <button @click="deleteRoom(room.id)">삭제</button>
            </div>
          </li>
      </ul>
    </section>

    <section v-if="currentView === 'hotelForm'">
      <div class="form-wrapper">
      <div class="header-actions">
          <button class="back-btn" @click="cancelHotelForm">← 뒤로가기</button>
          <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
          </div>
      </div>

      <div class="form-container">
        <h2>{{ editingHotel ? '호텔 수정' : '새 호텔 등록' }}</h2>
        <form @submit.prevent="handleHotelSubmit">
          <div class="form-group"><label>호텔명</label><input v-model="hotelForm.name" required /></div>
          <div class="form-group"><label>사업자번호 (선택)</label><input v-model.number="hotelForm.businessId" type="number" /></div>
          <div class="form-group"><label>주소</label><input v-model="hotelForm.address" required /></div>
          <div class="form-group"><label>국가</label><input v-model="hotelForm.country" required /></div>
          <div class="form-group"><label>성급⭐(1~5)</label><input v-model.number="hotelForm.starRating" type="number" min="1" max="5" required /></div>
          <div class="form-group"><label>호텔 설명</label><textarea v-model="hotelForm.description"></textarea></div>

          <div class="form-group">
            <label>이미지 (첫 번째 이미지가 대표 이미지)</label>
            <input type="file" @change="handleHotelFileChange" multiple accept="image/*" class="file-input">

            <draggable v-model="hotelEditableImages" item-key="id" class="image-preview-grid draggable-area" ghost-class="ghost">
              <template #item="{ element, index }">
                  <div class="image-preview-item">
                      <img :src="element.src" alt="이미지 프리뷰"/>
                      <span v-if="index === 0" class="main-photo-badge">대표</span>
                      <button type="button" class="btn-remove-img" @click="removeHotelImage(element, index)">X</button>
                  </div>
              </template>
            </draggable>
          </div>

          <div class="form-group">
            <label>편의시설</label>
            <div class="amenities-grid">
              <div v-for="amenity in allAmenities" :key="amenity.id" class="amenity-item">
                <input type="checkbox" :id="'amenity-' + amenity.id" :value="amenity.id" v-model="hotelForm.amenityIds" />
                <label :for="'amenity-' + amenity.id">{{ amenity.name }}</label>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn-primary">저장</button>
            <button type="button" class="btn-secondary" @click="cancelHotelForm">취소</button>
          </div>
        </form>
      </div>
    </div>
    </section>
    
    <section v-if="currentView === 'roomForm'">
      <div class="form-wrapper">
      <div class="header-actions">
          <button class="back-btn" @click="showRoomList(selectedHotel)">← 객실 목록으로</button>
          <div class="user-actions">
              <span v-if="user" class="user-name">{{ user.name }}님</span>
              <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
          </div>
      </div>
      <div class="form-container">
        <h2>{{ editingRoom ? '객실 수정' : '새 객실 등록' }}</h2>
          <form @submit.prevent="handleRoomSubmit">
            <div class="form-group">
              <label>객실 타입</label>
              <select v-model="roomForm.roomType" required>
                  <option disabled value="">객실 타입을 선택하세요</option>
                  <option>스위트룸</option> <option>디럭스룸</option> <option>스탠다드룸</option> <option>싱글룸</option> <option>트윈룸</option>
              </select>
            </div>
            <div class="form-group">
              <label>객실 크기</label>
              <div class="input-with-unit">
                <input v-model.number="roomSizeNumber" type="number" required placeholder="숫자만 입력" /> <span>㎡</span>
              </div>
            </div>
            <div class="form-group"><label>최소/최대 인원</label><div class="inline-group"><input v-model.number="roomForm.capacityMin" type="number" required /><input v-model.number="roomForm.capacityMax" type="number" required /></div></div>
            <div class="form-group">
              <label>1박 가격</label>
              <div class="input-with-unit">
                <input v-model.number="roomForm.price" type="number" required placeholder="숫자만 입력" /> <span>원</span>
              </div>
            </div>
            <div class="form-group"><label>객실 수</label><input v-model.number="roomForm.roomCount" type="number" required /></div>
            <div class="form-group"><label>체크인/체크아웃 시간</label><div class="inline-group"><input v-model="roomForm.checkInTime" type="time" required /><input v-model="roomForm.checkOutTime" type="time" required /></div></div>
            
            <div class="form-group">
              <label>이미지 (첫 번째 이미지가 대표 이미지)</label>
              <input type="file" @change="handleRoomFileChange" multiple accept="image/*" class="file-input">
              <draggable v-model="roomEditableImages" item-key="id" class="image-preview-grid draggable-area" ghost-class="ghost">
                <template #item="{ element, index }">
                    <div class="image-preview-item">
                        <img :src="element.src" alt="이미지 프리뷰"/>
                        <span v-if="index === 0" class="main-photo-badge">대표</span>
                        <button type="button" class="btn-remove-img" @click="removeRoomImage(element, index)">X</button>
                    </div>
                </template>
            </draggable>
            </div>

            <div class="form-actions">
              <button type="submit" class="btn-primary">저장</button>
              <button type="button" class="btn-secondary" @click="showRoomList(selectedHotel)">취소</button>
            </div>
          </form>
      </div>
      </div>
    </section>
  </div>
</template>

<script>
import axios from "axios";
import draggable from 'vuedraggable';

export default {
  name: 'OwnerHotel',
  components: { draggable },
  props: { user: Object },
  data() {
    return {
      myHotels: [], selectedHotel: null, rooms: [], editingHotel: null, editingRoom: null,
      hotelForm: {}, roomForm: {}, roomSizeNumber: null,
      hotelEditableImages: [], roomEditableImages: [], 
      newImageFiles: [], deletedImageUrls: [],
      allAmenities: [], currentView: 'list', 
    };
  },
  methods: {
    getAuthHeaders() {
      const token = localStorage.getItem('token');
      if (!token) { this.$router.push("/login"); return null; }
      return { 'Authorization': `Bearer ${token}` };
    },
    goToList() {
      this.selectedHotel = null; this.currentView = 'list'; this.fetchHotels();
    },
    async fetchHotels() {
      if (!this.user) return;
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const res = await axios.get(`/api/owner/hotels/my-hotels`, { headers });
        this.myHotels = res.data;
      } catch (err) { console.error("호텔 조회 실패:", err.response?.data || err.message); }
    },
    async fetchAmenities() { 
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        const response = await axios.get('/api/owner/hotels/amenities', { headers });
        this.allAmenities = response.data;
      } catch (err) { console.error("편의시설 목록 조회 실패:", err); }
    },
    async fetchRooms(hotelId) {
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        const res = await axios.get(`/api/owner/hotels/${hotelId}/rooms`, { headers });
        this.rooms = res.data;
      } catch (err) { console.error("객실 정보 조회 실패:", err); alert("객실 정보 조회에 실패했습니다."); }
    },
    showHotelDetails(hotel) { this.selectedHotel = hotel; this.currentView = 'details'; },
    async showRoomList(hotel) {
      this.selectedHotel = hotel; this.currentView = 'loading';
      await this.fetchRooms(hotel.id);
      this.currentView = 'rooms';
    },
    cancelHotelForm() {
      if (this.editingHotel) { this.currentView = 'details'; } 
      else { this.currentView = 'list'; }
      this.editingHotel = null;
    },
    openCreateForm() {
      this.editingHotel = null;
      this.hotelForm = { name: '', businessId: null, address: '', country: "대한민국", starRating: 5, description: '', amenityIds: [] };
      this.hotelEditableImages = []; this.newImageFiles = []; this.deletedImageUrls = [];
      this.currentView = 'hotelForm'; 
    },
    editHotel(hotel) {
      this.editingHotel = hotel;
      this.hotelForm = JSON.parse(JSON.stringify(hotel));
      this.hotelEditableImages = (hotel.imageUrls || []).map(url => ({ type: 'url', src: url, id: url }));
      this.newImageFiles = []; this.deletedImageUrls = [];
      this.currentView = 'hotelForm';
    },
    handleHotelFileChange(event) {
      const files = Array.from(event.target.files);
      files.forEach(file => {
        const previewUrl = URL.createObjectURL(file);
        const imageObject = { type: 'file', src: previewUrl, fileObject: file, id: previewUrl };
        this.hotelEditableImages.push(imageObject);
        this.newImageFiles.push(file);
      });
      event.target.value = '';
    },
    removeHotelImage(imageToRemove, index) {
      this.hotelEditableImages.splice(index, 1);
      if (imageToRemove.type === 'url') { this.deletedImageUrls.push(imageToRemove.src); } 
      else if (imageToRemove.type === 'file') {
        this.newImageFiles = this.newImageFiles.filter(f => f !== imageToRemove.fileObject);
        URL.revokeObjectURL(imageToRemove.src);
      }
    },
    handleHotelSubmit() {
      const formData = new FormData();
      const finalImageUrls = this.hotelEditableImages.filter(img => img.type === 'url').map(img => img.src);
      const hotelData = { ...this.hotelForm, imageUrls: finalImageUrls, deletedUrls: this.deletedImageUrls };
      formData.append('hotel', new Blob([JSON.stringify(hotelData)], { type: 'application/json' }));
      const newFilesInOrder = this.hotelEditableImages.filter(img => img.type === 'file').map(img => img.fileObject);
      newFilesInOrder.forEach(file => { formData.append('files', file); });
      if (this.editingHotel) { this.updateHotel(formData); } else { this.createHotel(formData); }
    },
    async createHotel(formData) {
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.post("/api/owner/hotels", formData, { headers });
        alert("호텔이 성공적으로 등록되었습니다."); this.goToList();
      } catch (err) { console.error("호텔 등록 실패:", err); alert("호텔 등록에 실패했습니다."); }
    },
    async updateHotel(formData) {
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.post(`/api/owner/hotels/${this.editingHotel.id}`, formData, { headers });
        alert("호텔 정보가 성공적으로 수정되었습니다."); this.goToList();
      } catch (err) { console.error("호텔 수정 실패:", err); alert("호텔 수정에 실패했습니다."); }
    },
    async deleteHotel(id) {
      if (!confirm("정말로 이 호텔을 삭제하시겠습니까? 연관된 모든 객실 정보도 함께 삭제됩니다.")) return;
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.delete(`/api/owner/hotels/${id}`, { headers });
        alert("호텔이 삭제되었습니다."); this.goToList();
      } catch (err) { console.error("호텔 삭제 실패:", err); alert("호텔 삭제에 실패했습니다."); }
    },
    openRoomCreateForm() {
        this.editingRoom = null; this.roomForm = { roomType: '스탠다드룸', checkInTime: '15:00', checkOutTime: '11:00' };
        this.roomSizeNumber = null; this.roomEditableImages = []; this.newImageFiles = []; this.deletedImageUrls = [];
        this.currentView = 'roomForm';
    },
    editRoom(room) {
        this.editingRoom = room; this.roomForm = JSON.parse(JSON.stringify(room));
        if (this.roomForm.roomSize) { this.roomSizeNumber = parseInt(this.roomForm.roomSize.replace(/[^0-9]/g, ''), 10); } 
        else { this.roomSizeNumber = null; }
        this.roomEditableImages = (room.imageUrls || []).map(url => ({ type: 'url', src: url, id: url }));
        this.newImageFiles = []; this.deletedImageUrls = []; this.currentView = 'roomForm';
    },
    handleRoomFileChange(event) {
      const files = Array.from(event.target.files);
      files.forEach(file => {
        const previewUrl = URL.createObjectURL(file);
        this.roomEditableImages.push({ type: 'file', src: previewUrl, fileObject: file, id: previewUrl });
        this.newImageFiles.push(file);
      });
      event.target.value = '';
    },
    removeRoomImage(imageToRemove, index) {
      this.roomEditableImages.splice(index, 1);
      if (imageToRemove.type === 'url') { this.deletedImageUrls.push(imageToRemove.src); } 
      else {
        this.newImageFiles = this.newImageFiles.filter(f => f !== imageToRemove.fileObject);
        URL.revokeObjectURL(imageToRemove.src);
      }
    },
    handleRoomSubmit() {
      if (this.roomSizeNumber) { this.roomForm.roomSize = `${this.roomSizeNumber}㎡`; }
      const formData = new FormData();
      const finalImageUrls = this.roomEditableImages.filter(img => img.type === 'url').map(img => img.src);
      const roomData = { ...this.roomForm, imageUrls: finalImageUrls, deletedUrls: this.deletedImageUrls };
      formData.append('room', new Blob([JSON.stringify(roomData)], { type: 'application/json' }));
      const newFilesInOrder = this.roomEditableImages.filter(img => img.type === 'file').map(img => img.fileObject);
      newFilesInOrder.forEach(file => { formData.append('files', file); });
      if (this.editingRoom) this.updateRoom(formData); else this.createRoom(formData);
    },
    async createRoom(formData) {
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.post(`/api/owner/hotels/${this.selectedHotel.id}/rooms`, formData, { headers});
        alert("객실이 등록되었습니다."); this.showRoomList(this.selectedHotel);
      } catch(err) { console.error("객실 등록 실패:", err); alert("객실 등록에 실패했습니다."); }
    },
    async updateRoom(formData) {
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.put(`/api/owner/hotels/rooms/${this.editingRoom.id}`, formData, { headers });
        alert("객실 정보가 수정되었습니다."); this.showRoomList(this.selectedHotel);
      } catch(err) { console.error("객실 수정 실패:", err); alert("객실 수정에 실패했습니다."); }
    },
    async deleteRoom(roomId) {
      if (!confirm("객실을 삭제하시겠습니까?")) return;
      const headers = this.getAuthHeaders(); if (!headers) return;
      try {
        await axios.delete(`/api/owner/hotels/rooms/${roomId}`, { headers });
        alert("객실이 삭제되었습니다."); this.fetchRooms(this.selectedHotel.id);
      } catch(err) { console.error("객실 삭제 실패:", err); alert("객실 삭제에 실패했습니다."); }
    },
  },
  mounted() {
    this.fetchHotels();
    this.fetchAmenities();
  }
}
</script>

<style scoped>
section { padding: 30px; }
h2 { margin: 0; font-size: 24px; color: #111827; }
h3 { margin-top: 20px; font-size: 20px; color: #111827; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.header-actions.secondary { margin-top: 20px; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid #e5e7eb; }
.user-actions { display: flex; align-items: center; gap: 15px; }
.user-name { font-weight: 600; color: #374151; }
.add-btn { padding: 10px 16px; background: #3b82f6; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
.add-btn:hover { background: #2563eb; }
.logout-btn { padding: 10px 16px; background: #6b7280; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
.logout-btn:hover { background: #4b5563; }
.back-btn { margin: 0; padding: 10px 16px; background: #6b7280; color: #fff; border: none; border-radius: 6px; cursor: pointer; }
.back-btn:hover { background: #4b5563; }
.hotel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 25px; }
.hotel-card { aspect-ratio: 1/1; background: #fff; border-radius: 12px; box-shadow: 0 4px 12px #00000014; cursor: pointer; overflow: hidden; display: flex; flex-direction: column; transition: transform .2s, box-shadow .2s; }
.hotel-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px #0000001f; }
.hotel-card-image { width: 100%; height: 70%; object-fit: cover; }
.hotel-card-info { padding: 15px; flex-grow: 1; display: flex; flex-direction: column; }
.hotel-card-info strong { font-size: 18px; margin-bottom: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hotel-card-info span { font-size: 14px; color: #6b7280; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hotel-details-view { background: #fff; border-radius: 12px; padding: 30px; display: flex; gap: 30px; border: 1px solid #e5e7eb; }
.details-image { width: 400px; height: 250px; object-fit: cover; border-radius: 10px; flex-shrink: 0; }
.details-info { display: flex; flex-direction: column; }
.details-info h2 { margin-top: 0; }
.details-info p { font-size: 16px; color: #374151; line-height: 1.6; }
.details-actions { margin-top: auto; padding-top: 20px; display: flex; gap: 15px; }
.details-actions button { padding: 12px 24px; font-size: 16px; font-weight: 700; border-radius: 8px; border: none; cursor: pointer; transition: background-color .2s; }
.btn-edit { background-color: #3b82f6; color: #fff; }
.btn-edit:hover { background-color: #2563eb; }
.btn-delete { background-color: #ef4444; color: #fff; }
.btn-delete:hover { background-color: #dc2626; }
.btn-rooms { background-color: #10b981; color: #fff; }
.btn-rooms:hover { background-color: #059669; }
.room-list { list-style: none; padding: 0; margin: 0; }
.room-item { background: #fff; padding: 15px; margin-bottom: 10px; border-radius: 8px; border: 1px solid #e5e7eb; display: flex; align-items: center; gap: 15px; }
.room-image { width: 120px; height: 90px; border-radius: 6px; object-fit: cover; }
.room-info { flex: 1; display: flex; flex-direction: column; }
.room-info strong { font-size: 16px; }
.room-info span { font-size: 14px; color: #6b7280; }
.actions button { margin-left: 5px; padding: 6px 10px; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.actions button:first-child { background: #3b82f6; color: #fff; }
.actions button:last-child { background: #ef4444; color: #fff; }
.form-wrapper { max-width: 800px; margin: 0 auto; }
.form-container { background: #fff; padding: 30px; border-radius: 12px; border: 1px solid #e5e7eb; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #374151; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 12px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; box-sizing: border-box; }
.form-group textarea { resize: vertical; min-height: 120px; }
.inline-group { display: flex; gap: 10px; }
.form-actions { margin-top: 30px; display: flex; justify-content: flex-end; gap: 12px; }
.form-actions button { padding: 12px 20px; border-radius: 6px; border: none; cursor: pointer; font-size: 14px; font-weight: 700; }
.btn-primary { background: #3b82f6; color: #fff; }
.btn-secondary { background: #e5e7eb; color: #374151; }
.btn-secondary:hover { background: #d1d5db; }
.file-input { width: 100%; padding: 8px; border: 1px solid #d1d5db; border-radius: 6px; background-color: white; }
.image-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; margin-top: 15px; }
.image-preview-item { position: relative; aspect-ratio: 4 / 3; }
.image-preview-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 6px; border: 1px solid #e5e7eb; }
.btn-remove-img { position: absolute; top: 5px; right: 5px; width: 24px; height: 24px; border-radius: 50%; border: none; background-color: rgba(0, 0, 0, 0.6); color: white; cursor: pointer; font-weight: bold; display: flex; align-items: center; justify-content: center; line-height: 1; }
.amenities-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; background-color: #f9fafb; padding: 15px; border-radius: 8px; border: 1px solid #e5e7eb; }
.amenity-item { display: flex; align-items: center; gap: 8px; }
.amenity-item input[type="checkbox"] { width: 16px; height: 16px; cursor: pointer; }
.amenity-item label { font-size: 14px; color: #374151; margin-bottom: 0; cursor: pointer; }
.draggable-area { cursor: grab; }
.ghost { opacity: 0.5; background: #c8ebfb; }
.main-photo-badge { position: absolute; top: 5px; left: 5px; background-color: #3b82f6; color: white; padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: 700; z-index: 2; }
.input-with-unit { display: flex; align-items: center; border: 1px solid #d1d5db; border-radius: 6px; overflow: hidden; }
.input-with-unit input { border: none; flex-grow: 1; }
.input-with-unit input:focus { outline: none; box-shadow: none; }
.input-with-unit span { padding: 0 12px; color: #6b7280; background-color: #f9fafb; border-left: 1px solid #d1d5db; align-self: stretch; display: flex; align-items: center; }
</style>