import React, { useState, useEffect, useMemo, useCallback, useRef } from "react";
import axios from "axios";

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || "/api";
const SLOT_HOURS = Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, '0'));
const SLOT_MINUTES = Array.from({ length: 60 }, (_, index) => String(index).padStart(2, '0'));

const parse24HourTime = (timeValue) => {
  if (!timeValue) {
    return { hour: '', minute: '', period: 'AM' };
  }

  const [rawHour = '00', rawMinute = '00'] = timeValue.split(':');
  const hourNumber = Number(rawHour);

  if (Number.isNaN(hourNumber)) {
    return { hour: '', minute: '', period: 'AM' };
  }

  const period = hourNumber >= 12 ? 'PM' : 'AM';
  const hour12 = hourNumber % 12 || 12;

  return {
    hour: String(hour12).padStart(2, '0'),
    minute: String(rawMinute).padStart(2, '0'),
    period
  };
};

const format12HourTo24Hour = ({ hour, minute, period }) => {
  if (!hour || minute === '' || !period) {
    return '';
  }

  let normalizedHour = Number(hour) % 12;
  if (period === 'PM') {
    normalizedHour += 12;
  }

  return `${String(normalizedHour).padStart(2, '0')}:${String(Number(minute)).padStart(2, '0')}`;
};

const Notification = ({ message, type, onClose, persistent }) => {
  useEffect(() => {
    if (!message || persistent) return;
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [message, onClose, persistent]);

  if (!message) return null;

  return (
    <div className={`fixed top-5 right-5 p-4 rounded-lg shadow-lg text-white z-50 max-w-md ${type==='success'?'bg-green-500':'bg-red-500'}`}>
      <div className="flex items-start justify-between">
        <span className="flex-1">{message}</span>
        <button onClick={onClose} className="ml-4 font-bold text-xl hover:opacity-80">✕</button>
      </div>
    </div>
  );
};

const StatusBadge = ({ status }) => {
  const statusStyles = {
    Active: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100',
    Blocked: 'bg-red-50 text-red-700 ring-1 ring-red-100',
    'Completed Exam': 'bg-blue-50 text-blue-700 ring-1 ring-blue-100',
    'Assigned Exam': 'bg-amber-50 text-amber-700 ring-1 ring-amber-100',
  };
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${statusStyles[status] || 'bg-gray-100 text-gray-800'}`}>
      {status}
    </span>
  );
};

export default function ManageUsers() {
  const ITEMS_PER_PAGE = 5;
  const isAdminUser = (user) =>
    Array.isArray(user?.roles) &&
    user.roles.some(role => (role?.name || '').toUpperCase() === 'ADMIN');

  const [allUsers, setAllUsers] = useState([]);
  const [slots, setSlots] = useState([]);
  const [currentSlotId, setCurrentSlotId] = useState(null);
  const [exams, setExams] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isAssignExamModalOpen, setIsAssignExamModalOpen] = useState(false);
  const [isSlotModalOpen, setIsSlotModalOpen] = useState(false);
  const [isDeleteSlotModalOpen, setIsDeleteSlotModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);
  const [slotToDelete, setSlotToDelete] = useState(null);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [sortConfig, setSortConfig] = useState({ key: 'id', direction: 'ascending' });
  const [notification, setNotification] = useState({ message: '', type: '', persistent: false });
  const [currentPage, setCurrentPage] = useState(1);
  const [formData, setFormData] = useState({ name: '', email: '', collegeName: '', password: '', status: 'Active' });
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [backendError, setBackendError] = useState(false);
  const [slotFormData, setSlotFormData] = useState({ slotNumber: '', date: '', time: '', collegeName: '', slotPassword: '', passPercentage: '80' });
  const [slotTimeInput, setSlotTimeInput] = useState({ hour: '', minute: '', period: 'AM' });
  const [isSlotSubmitting, setIsSlotSubmitting] = useState(false);
  const [editingSlot, setEditingSlot] = useState(null);
  const [isExcelUploading, setIsExcelUploading] = useState(false);
  const excelFileInputRef = useRef(null);
  const selectAllCheckboxRef = useRef(null);

  const fetchUsers = useCallback(async () => {
    try {
      setIsLoading(true);
      setBackendError(false);

      const res = await axios.get(`${API_BASE_URL}/admin/users`, { withCredentials: true });
      setAllUsers(res.data);
      setSelectedUsers([]);
      setIsLoading(false);
    } catch (err) { 
      console.error('Fetch users error:', err);
      setIsLoading(false);
      setBackendError(true);
      
      let errorMessage = 'Failed to fetch users. ';
      if (err.code === 'ERR_NETWORK' || err.message === 'Network Error') {
        errorMessage += 'Cannot connect to backend server. Please ensure the server is running and accessible.';
      } else if (err.response) {
        errorMessage += err.response.data?.message || `Server error: ${err.response.status}`;
      } else {
        errorMessage += 'Please check your connection and try again.';
      }
      
      setNotification({ message: errorMessage, type: 'error', persistent: true });
    }
  }, []);

  const fetchSlots = async () => {
    try {
      const res = await axios.get(`${API_BASE_URL}/admin/slots`);
      setSlots(res.data);
      if (res.data.length > 0 && !currentSlotId) {
        setCurrentSlotId(res.data[0].id);
      }
    } catch (err) { 
      console.error('Fetch slots error:', err);
      setNotification({ message: 'Failed to fetch slots', type: 'error', persistent: false });
    }
  };

  const fetchExams = async () => {
    try {
      const res = await axios.get(`${API_BASE_URL}/admin/exams`);
      const examsData = Array.isArray(res.data)
        ? res.data
        : Array.isArray(res.data?.exams)
          ? res.data.exams
          : [];
      setExams(examsData);
    } catch (err) { 
      console.error('Fetch exams error:', err);
      setExams([]);
      setNotification({ message: 'Failed to fetch exams', type: 'error', persistent: false });
    }
  };

  useEffect(() => { 
    fetchUsers(); 
    fetchSlots();
    fetchExams(); 
  }, [fetchUsers]);

  useEffect(() => {
    document.body.classList.add('admin-dashboard-body');
    return () => {
      document.body.classList.remove('admin-dashboard-body');
    };
  }, []);

  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(null, "", window.location.href);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const buildSlotStartDateTime = () => {
    const slot = slots.find(s => s.id === currentSlotId);
    if (!slot || !slot.date || !slot.time) return null;
    return `${slot.date}T${slot.time}:00`;
  };

  const addNewSlot = () => {
    setEditingSlot(null);
    setSlotFormData({ slotNumber: '', date: '', time: '', collegeName: '', slotPassword: '', passPercentage: '80' });
    setSlotTimeInput({ hour: '', minute: '', period: 'AM' });
    setIsSlotModalOpen(true);
  };

  const openEditSlotModal = (slot) => {
    setEditingSlot(slot);
    const parsedTime = parse24HourTime(slot.time || '');
    setSlotFormData({
      slotNumber: slot.slotNumber.toString(),
      date: slot.date || '',
      time: slot.time || '',
      collegeName: slot.collegeName || '',
      slotPassword: slot.slotPassword || '',
      passPercentage: String(slot.passPercentage ?? 80)
    });
    setSlotTimeInput(parsedTime);
    setIsSlotModalOpen(true);
  };

  const handleSlotTimeChange = (field, value) => {
    const nextTimeInput = { ...slotTimeInput, [field]: value };
    setSlotTimeInput(nextTimeInput);
    setSlotFormData(current => ({
      ...current,
      time: format12HourTo24Hour(nextTimeInput)
    }));
  };

  const handleCreateOrUpdateSlot = async () => {
    if (!slotFormData.slotNumber || !slotFormData.date || !slotFormData.time || !slotFormData.slotPassword || slotFormData.passPercentage === '') {
      setNotification({ message: 'Please fill in slot number, date, time, slot password, and pass percentage', type: 'error', persistent: false });
      return;
    }

    const passPercentage = parseInt(slotFormData.passPercentage, 10);
    if (Number.isNaN(passPercentage) || passPercentage < 0 || passPercentage > 100) {
      setNotification({ message: 'Pass percentage must be between 0 and 100', type: 'error', persistent: false });
      return;
    }

    setIsSlotSubmitting(true);
    try {
      const payload = {
        slotNumber: parseInt(slotFormData.slotNumber),
        date: slotFormData.date,
        time: slotFormData.time,
        collegeName: slotFormData.collegeName || null,
        slotPassword: slotFormData.slotPassword,
        passPercentage
      };

      if (editingSlot) {
        await axios.put(`${API_BASE_URL}/admin/slots/${editingSlot.id}`, payload);
        setNotification({ message: 'Slot updated successfully!', type: 'success', persistent: false });
      } else {
        await axios.post(`${API_BASE_URL}/admin/slots`, payload);
        setNotification({ message: 'Slot created successfully!', type: 'success', persistent: false });
      }

      await fetchSlots();
      setIsSlotModalOpen(false);
      setEditingSlot(null);
    } catch (err) {
      console.error('Create/Update slot error:', err);
      const errorMsg = err.response?.data?.error || `Failed to ${editingSlot ? 'update' : 'create'} slot`;
      setNotification({ message: errorMsg, type: 'error', persistent: false });
    } finally {
      setIsSlotSubmitting(false);
    }
  };

  const openDeleteSlotModal = (slot) => {
    setSlotToDelete(slot);
    setIsDeleteSlotModalOpen(true);
  };

  const closeDeleteSlotModal = () => {
    setSlotToDelete(null);
    setIsDeleteSlotModalOpen(false);
  };

  const handleDeleteSlot = async () => {
    if (!slotToDelete) return;

    try {
      await axios.delete(`${API_BASE_URL}/admin/slots/${slotToDelete.id}`);
      setNotification({ message: 'Slot deleted successfully', type: 'success', persistent: false });
      
      await fetchSlots();
      await fetchUsers();
      
      if (currentSlotId === slotToDelete.id) {
        const remainingSlots = slots.filter(s => s.id !== slotToDelete.id);
        if (remainingSlots.length > 0) {
          setCurrentSlotId(remainingSlots[0].id);
        } else {
          setCurrentSlotId(null);
        }
      }
      
      closeDeleteSlotModal();
    } catch (err) {
      console.error('Delete slot error:', err);
      const errorMsg = err.response?.data?.error || 'Failed to delete slot';
      setNotification({ message: errorMsg, type: 'error', persistent: false });
    }
  };

  const openModal = (user=null) => {
    setEditingUser(user);
    setFormData(user ? { ...user } : { name:'', email:'', collegeName:'', password:'', status:'Active' });
    setFormError('');
    setIsModalOpen(true);
  };
  
  const closeModal = () => {
    setIsModalOpen(false);
    setFormError('');
    setIsSubmitting(false);
  };

  const openDeleteModal = (user) => { setUserToDelete(user); setIsDeleteModalOpen(true); };
  const closeDeleteModal = () => setIsDeleteModalOpen(false);

  const openAssignExamModal = () => setIsAssignExamModalOpen(true);
  const closeAssignExamModal = () => { setIsAssignExamModalOpen(false); setSelectedExamId(''); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setIsSubmitting(true);

    try {
      if (editingUser) {
        await axios.put(`${API_BASE_URL}/admin/users/${editingUser.id}`, formData);
        setNotification({ message: 'User updated successfully!', type:'success', persistent: false });
      } else {
        const currentSlot = slots.find(s => s.id === currentSlotId);
        const payload = {
          ...formData,
          slotNumber: currentSlot?.slotNumber
        };
        await axios.post(`${API_BASE_URL}/auth/addCandidate`, payload);
        setNotification({ message: 'User added successfully!', type:'success', persistent: false });
      }
      
      await fetchUsers();
      closeModal();
    } catch(err){
      console.error('Error saving user:', err);
      
      if (err.response) {
        const errorMessage = err.response.data?.message || err.response.data?.error;
        
        if (err.response.status === 400 || err.response.status === 409) {
          if (errorMessage && errorMessage.toLowerCase().includes('email')) {
            setFormError('This email is already registered. Please use a different email.');
          } else if (errorMessage) {
            setFormError(errorMessage);
          } else {
            setFormError('Failed to save user. Please check your input.');
          }
        } else if (err.response.status === 500) {
          setFormError('Server error. This email may already be registered.');
        } else {
          setFormError('An unexpected error occurred. Please try again.');
        }
      } else if (err.request) {
        setFormError('No response from server. Please check your connection.');
      } else {
        setFormError('Failed to save user. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    try {
      await axios.delete(`${API_BASE_URL}/admin/users/${userToDelete.id}`);
      setNotification({ message: `User ${userToDelete.name} deleted successfully.`, type:'success', persistent: false });
      await fetchUsers();
      closeDeleteModal();
    } catch(err){ 
      console.error('Delete error:', err);
      setNotification({ message: 'Failed to delete user', type: 'error', persistent: false });
    }
  };

  const handleAssignExam = async (examId) => {
    if (!examId || selectedUsers.length === 0) return;

    const slotStartTime = buildSlotStartDateTime();

    if (!slotStartTime) {
      setNotification({
        message: "Selected slot does not have valid date & time",
        type: "error",
        persistent: false
      });
      return;
    }

    try {
      const payload = {
        examId: Number(examId),
        userIds: selectedUsers.map(id => Number(id)),
        slotStartTime
      };

      const response = await axios.post(`${API_BASE_URL}/admin/users/assign-exam`, payload);

      setNotification({
        message:
          response.data?.message ||
          "Exam assigned successfully and email queued for selected users.",
        type: "success",
        persistent: false
      });

      setSelectedUsers([]);
      await fetchUsers();
      closeAssignExamModal();

    } catch (err) {
      console.error("Assign Exam Error:", err);
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Failed to assign exam";

      setNotification({
        message: errorMsg,
        type: "error",
        persistent: false
      });
    }
  };

  const handleBulkDelete = async () => {
    if (!window.confirm(`Are you sure you want to delete ${selectedUsers.length} user(s)?`)) {
      return;
    }
    
    try {
      await Promise.all(selectedUsers.map(id => axios.delete(`${API_BASE_URL}/admin/users/${id}`)));
      setNotification({ message: `${selectedUsers.length} user(s) deleted successfully.`, type:'success', persistent: false });
      setSelectedUsers([]);
      await fetchUsers();
    } catch(err){ 
      console.error('Bulk delete error:', err);
      setNotification({ message: 'Failed to delete some users', type: 'error', persistent: false });
    }
  };

  const triggerExcelUpload = () => {
    if (!currentSlotId) {
      setNotification({ message: 'Please create and select a slot first', type: 'error', persistent: false });
      return;
    }
    excelFileInputRef.current?.click();
  };

  const handleExcelUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      setNotification({ message: 'Please upload a valid Excel file (.xlsx or .xls)', type: 'error', persistent: false });
      e.target.value = '';
      return;
    }

    if (!currentSlotId) {
      setNotification({ message: 'Please select a slot before uploading', type: 'error', persistent: false });
      e.target.value = '';
      return;
    }

    setIsExcelUploading(true);
    try {
      const uploadFormData = new FormData();
      uploadFormData.append('file', file);
      uploadFormData.append('slotId', currentSlotId);

      const res = await axios.post(`${API_BASE_URL}/admin/users/upload-excel`, uploadFormData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        withCredentials: true
      });

      const createdCount = res.data?.createdCount || 0;
      const failedCount = res.data?.failedCount || 0;
      const failedRows = Array.isArray(res.data?.failedRows) ? res.data.failedRows : [];

      let message = `Upload complete: ${createdCount} user(s) created in the selected slot`;
      if (failedCount > 0) {
        const preview = failedRows.slice(0, 2).join(' | ');
        message += `. ${failedCount} row(s) failed${preview ? ` (${preview}${failedRows.length > 2 ? ' ...' : ''})` : ''}`;
      }

      setNotification({
        message,
        type: failedCount > 0 ? 'error' : 'success',
        persistent: false
      });

      await fetchUsers();
    } catch (err) {
      console.error('Excel upload error:', err);
      const errorMsg = err.response?.data?.error || 'Failed to upload users from Excel';
      setNotification({ message: errorMsg, type: 'error', persistent: false });
    } finally {
      setIsExcelUploading(false);
      e.target.value = '';
    }
  };

  const requestSort = (key) => {
    let direction='ascending';
    if(sortConfig.key===key && sortConfig.direction==='ascending') direction='descending';
    setSortConfig({ key, direction });
  };

  const sortedAndFilteredUsers = useMemo(() => {
    if (!Array.isArray(allUsers)) return [];
    
    const currentSlot = slots.find(s => s.id === currentSlotId);
    const usersInSlot = currentSlotId 
      ? allUsers.filter(u => !isAdminUser(u) && u.slotNumber === currentSlot?.slotNumber)
      : allUsers.filter(u => !isAdminUser(u));

    let filtered = usersInSlot.filter(u =>
      (u.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
       u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
       (u.collegeName && u.collegeName.toLowerCase().includes(searchTerm.toLowerCase()))) &&
      (statusFilter==='All' || u.status===statusFilter)
    );

    filtered.sort((a,b)=>{
      const aVal = a[sortConfig.key] || '';
      const bVal = b[sortConfig.key] || '';
      if(aVal<bVal) return sortConfig.direction==='ascending'?-1:1;
      if(aVal>bVal) return sortConfig.direction==='ascending'?1:-1;
      return 0;
    });
    return filtered;
  }, [allUsers, currentSlotId, slots, searchTerm, statusFilter, sortConfig]);

  const totalPages = Math.ceil(sortedAndFilteredUsers.length / ITEMS_PER_PAGE);
  const paginatedUsers = useMemo(() => {
    const start=(currentPage-1)*ITEMS_PER_PAGE;
    return sortedAndFilteredUsers.slice(start, start+ITEMS_PER_PAGE);
  }, [currentPage, sortedAndFilteredUsers]);

  const handleSelectUser = (id) => {
    const selectedUser = allUsers.find(u => u.id === id);
    if (selectedUser && isAdminUser(selectedUser)) return;
    setSelectedUsers(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };
  const handleSelectAllAcrossPages = (e) => {
    const filteredIds = sortedAndFilteredUsers.filter(u => !isAdminUser(u)).map(u => u.id);
    if (e.target.checked) {
      setSelectedUsers(prev => [...new Set([...prev, ...filteredIds])]);
    } else {
      setSelectedUsers(prev => prev.filter(id => !filteredIds.includes(id)));
    }
  };

  const selectableUsersAcrossPages = useMemo(
    () => sortedAndFilteredUsers.filter(u => !isAdminUser(u)),
    [sortedAndFilteredUsers]
  );
  const selectableUserIdsAcrossPages = useMemo(
    () => selectableUsersAcrossPages.map(u => u.id),
    [selectableUsersAcrossPages]
  );
  const areAllAcrossPagesSelected =
    selectableUserIdsAcrossPages.length > 0 &&
    selectableUserIdsAcrossPages.every(id => selectedUsers.includes(id));
  const areSomeAcrossPagesSelected =
    selectableUserIdsAcrossPages.some(id => selectedUsers.includes(id)) &&
    !areAllAcrossPagesSelected;

  useEffect(() => {
    if (selectAllCheckboxRef.current) {
      selectAllCheckboxRef.current.indeterminate = areSomeAcrossPagesSelected;
    }
  }, [areSomeAcrossPagesSelected]);

  const handleSlotChange = (slotId) => {
    setCurrentSlotId(slotId);
    setCurrentPage(1);
    setSelectedUsers([]);
    setSearchTerm('');
    setStatusFilter('All');
  };

  useEffect(()=>{ 
    if(currentPage>totalPages && totalPages>0) setCurrentPage(totalPages); 
  },[currentPage,totalPages]);

  const currentSlot = slots.find(s => s.id === currentSlotId);

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const formatTime = (timeString) => {
    if (!timeString) return 'N/A';
    const [hourPart = '0', minutePart = '00'] = String(timeString).split(':');
    const hour24 = Number.parseInt(hourPart, 10);

    if (Number.isNaN(hour24)) {
      return timeString;
    }

    const period = hour24 >= 12 ? 'PM' : 'AM';
    const hour12 = hour24 % 12 || 12;
    return `${String(hour12).padStart(2, '0')}:${String(minutePart).padStart(2, '0')} ${period}`;
  };

  return (
    <div className="min-h-screen">
      <Notification 
        message={notification.message} 
        type={notification.type} 
        persistent={notification.persistent}
        onClose={()=>setNotification({message:'',type:'', persistent: false})}
      />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        <div className="relative overflow-hidden rounded-3xl border border-white/70 bg-white/80 backdrop-blur-sm shadow-[0_30px_80px_-50px_rgba(15,23,42,0.45)] p-6 sm:p-8">
          <div className="pointer-events-none absolute -top-24 right-0 h-48 w-48 rounded-full bg-blue-200/40 blur-3xl"></div>
          <div className="pointer-events-none absolute -bottom-24 -left-16 h-40 w-40 rounded-full bg-indigo-200/40 blur-3xl"></div>

          <div className="relative z-10">
            <div className="mb-6 sm:mb-8">
              <span className="inline-flex items-center gap-2 rounded-full border border-blue-200/60 bg-blue-50/70 px-3 py-1 text-xs font-semibold uppercase tracking-widest text-blue-700">
                User management
              </span>
              <h2 className="mt-3 text-2xl sm:text-3xl lg:text-4xl font-semibold text-slate-900">Manage Users</h2>
              <p className="mt-2 text-sm sm:text-base text-slate-600">
                View, edit, and manage all registered users across exam slots.
              </p>
            </div>

      {backendError && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6 rounded-xl">
          <div className="flex items-start">
            <svg className="w-6 h-6 text-red-500 mr-3 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
            <div className="flex-1">
              <h3 className="text-red-800 font-semibold">Failed to load users</h3>
              <p className="text-red-700 text-sm mt-1">Cannot connect to the backend server. Please ensure:</p>
              <ul className="text-red-700 text-sm mt-2 ml-4 list-disc">
                <li>The backend server is running and accessible</li>
                <li>CORS is properly configured on the backend</li>
                <li>The API endpoints are accessible (check console for URLs)</li>
              </ul>
              <button 
                onClick={fetchUsers}
                className="mt-3 rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-red-700"
              >
                Retry Connection
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="mb-6 rounded-2xl border border-white/70 bg-white/80 p-6 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-lg font-semibold text-slate-900">Exam Slots</h3>
            <p className="text-xs text-slate-500">Create, edit, and manage slot schedules.</p>
          </div>
          <button
            onClick={addNewSlot}
            className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Add Slot
          </button>
        </div>
        
        {slots.length === 0 ? (
          <div className="text-center py-8 text-slate-500">
            <p>No slots available. Create your first slot to get started.</p>
          </div>
        ) : (
          <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {slots.map(slot => {
              const count = Array.isArray(allUsers) ? allUsers.filter(u => u.slotNumber === slot.slotNumber).length : 0;
              return (
                <div
                  key={slot.id}
                  className={`group relative cursor-pointer overflow-hidden rounded-2xl border border-white/70 bg-white/80 p-4 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md ${
                    currentSlotId === slot.id
                      ? 'ring-2 ring-blue-200'
                      : 'ring-1 ring-transparent'
                  }`}
                  onClick={() => handleSlotChange(slot.id)}
                >
                  <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-blue-500 via-cyan-400 to-indigo-400"></div>
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="text-lg font-semibold text-slate-900">Slot {slot.slotNumber}</h4>
                      {slot.collegeName && (
                        <p className="mt-1 text-sm font-medium text-slate-600">{slot.collegeName}</p>
                      )}
                      {slot.date && slot.time && (
                        <div className="mt-3 space-y-1.5">
                          <div className="flex items-center text-sm text-slate-600">
                            <svg className="mr-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            {formatDate(slot.date)}
                          </div>
                          <div className="flex items-center text-sm text-slate-600">
                            <svg className="mr-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            {formatTime(slot.time)}
                          </div>
                          <div className="flex items-center text-sm text-slate-600">
                            <svg className="mr-1 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-6a3 3 0 016 0v6m-8 0h10" />
                            </svg>
                            Pass: {slot.passPercentage ?? 80}%
                          </div>
                        </div>
                      )}
                      <div className="mt-2">
                        <span className={`inline-block px-2 py-1 rounded-full text-xs font-semibold ${
                          currentSlotId === slot.id ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-600'
                        }`}>
                          {count} {count === 1 ? 'user' : 'users'}
                        </span>
                      </div>
                    </div>
                    <div className="flex gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openEditSlotModal(slot);
                        }}
                        className="text-blue-600 hover:text-blue-800"
                        title="Edit slot"
                      >
                        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          openDeleteSlotModal(slot);
                        }}
                        className="text-red-500 hover:text-red-700"
                        title="Delete slot"
                      >
                        <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="mb-6 rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
        <div className="flex flex-wrap items-center gap-4 justify-between">
          <div className="flex flex-1 flex-wrap gap-3">
            <input 
              type="text" 
              placeholder="Search by name, email, or college..." 
              value={searchTerm} 
              onChange={e=>setSearchTerm(e.target.value)} 
              className="min-w-[220px] flex-1 rounded-xl border border-slate-200 bg-white/90 px-4 py-2 text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <select 
              value={statusFilter} 
              onChange={e=>setStatusFilter(e.target.value)} 
              className="rounded-xl border border-slate-200 bg-white/90 px-4 py-2 text-sm text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="All">All Statuses</option>
              <option value="Active">Active</option>
              <option value="Blocked">Blocked</option>
              <option value="Completed Exam">Completed Exam</option>
              <option value="Assigned Exam">Assigned Exam</option>
            </select>
          </div>
          <div className="flex flex-wrap gap-2">
          {selectedUsers.length>0 && (
            <>
              <button 
                onClick={handleBulkDelete} 
                className="rounded-xl bg-red-500 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-red-600"
              >
                Delete ({selectedUsers.length})
              </button>
              <button 
                onClick={openAssignExamModal} 
                className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600"
              >
                Assign Exam ({selectedUsers.length})
              </button>
            </>
          )}
          <button
            onClick={triggerExcelUpload}
            disabled={!currentSlotId || isExcelUploading}
            className="rounded-xl bg-indigo-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            title={!currentSlotId ? "Please create a slot first" : "Upload users Excel for selected slot"}
          >
            {isExcelUploading ? 'Uploading...' : 'Upload Excel'}
          </button>
          <button 
            onClick={()=>openModal()} 
            disabled={!currentSlotId}
            className="rounded-xl bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            title={!currentSlotId ? "Please create a slot first" : ""}
          >
            + Add User
          </button>
          <input
            ref={excelFileInputRef}
            type="file"
            accept=".xlsx,.xls"
            className="hidden"
            onChange={handleExcelUpload}
          />
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="rounded-2xl border border-white/70 bg-white/80 p-12 text-center shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-slate-600">Loading users...</p>
        </div>
      ) : sortedAndFilteredUsers.length === 0 ? (
        <div className="rounded-2xl border border-white/70 bg-white/80 p-12 text-center shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
          <svg className="mx-auto h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <h3 className="mt-4 text-lg font-semibold text-slate-700">No users found</h3>
          <p className="mt-2 text-slate-500">
            {searchTerm || statusFilter !== 'All' 
              ? 'Try adjusting your search or filters' 
              : currentSlot ? `No users in Slot ${currentSlot.slotNumber} yet` : 'Please create a slot first'}
          </p>
          {!searchTerm && statusFilter === 'All' && currentSlotId && (
            <button 
              onClick={()=>openModal()} 
              className="mt-4 rounded-xl bg-blue-600 px-6 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-700"
            >
              Add First User
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-2xl border border-white/70 bg-white/80 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
            <table className="min-w-full">
              <thead className="bg-slate-50/80 text-slate-500 uppercase text-xs tracking-wider">
                <tr>
                  <th className="py-3 px-4 text-left">
                    <input
                      ref={selectAllCheckboxRef}
                      type="checkbox"
                      onChange={handleSelectAllAcrossPages}
                      checked={areAllAcrossPagesSelected}
                      className="cursor-pointer"
                      title="Select all users across all pages"
                    />
                  </th>
                  <th className="py-3 px-4 text-left">Sl No.</th>
                  <th className="py-3 px-4 text-left cursor-pointer hover:bg-slate-100" onClick={()=>requestSort('id')}>
                    ID {sortConfig.key==='id' && (sortConfig.direction==='ascending'?'↑':'↓')}
                  </th>
                  <th className="py-3 px-6 text-left cursor-pointer hover:bg-slate-100" onClick={()=>requestSort('name')}>
                    Name {sortConfig.key==='name' && (sortConfig.direction==='ascending'?'↑':'↓')}
                  </th>
                  <th className="py-3 px-6 text-left cursor-pointer hover:bg-slate-100" onClick={()=>requestSort('collegeName')}>
                    College {sortConfig.key==='collegeName' && (sortConfig.direction==='ascending'?'↑':'↓')}
                  </th>
                  <th className="py-3 px-6 text-left cursor-pointer hover:bg-slate-100" onClick={()=>requestSort('email')}>
                    Email {sortConfig.key==='email' && (sortConfig.direction==='ascending'?'↑':'↓')}
                  </th>
                  <th className="py-3 px-6 text-left">Password</th>
                  <th className="py-3 px-6 text-left cursor-pointer hover:bg-slate-100" onClick={()=>requestSort('status')}>
                    Status {sortConfig.key==='status' && (sortConfig.direction==='ascending'?'↑':'↓')}
                  </th>
                  <th className="py-3 px-6 text-left">Assigned Exams</th>
                  <th className="py-3 px-6 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="text-slate-700 text-sm">
                {paginatedUsers.map((u, idx) => (
                  <tr key={u.id} className="border-b border-slate-100 hover:bg-slate-50/70 transition">
                    <td className="py-3 px-4">
                      <input 
                        type="checkbox" 
                        checked={selectedUsers.includes(u.id)} 
                        onChange={()=>handleSelectUser(u.id)}
                        className="cursor-pointer disabled:cursor-not-allowed"
                        disabled={isAdminUser(u)}
                        title={isAdminUser(u) ? "Admin user cannot be selected for candidate actions" : ""}
                      />
                    </td>
                    <td className="py-3 px-4 font-medium">
                      {((currentPage - 1) * ITEMS_PER_PAGE) + idx + 1}
                    </td>
                    <td className="py-3 px-4">{u.id}</td>
                    <td className="py-3 px-6 font-semibold">{u.name}</td>
                    <td className="py-3 px-6">{u.collegeName || 'N/A'}</td>
                    <td className="py-3 px-6">{u.email}</td>
                    <td className="py-3 px-6 font-mono text-xs">{u.password}</td>
                    <td className="py-3 px-6"><StatusBadge status={u.status}/></td>
                    <td className="py-3 px-6">
                      {u.assignedExams?.length > 0
                        ? u.assignedExams.map(ex => ex.title).join(', ')
                        : <span className="text-gray-400 italic">None</span>}
                    </td>
                    <td className="py-3 px-6 text-center">
                      <div className="flex gap-3 justify-center">
                        <button 
                          onClick={()=>openModal(u)} 
                          className="text-blue-600 hover:text-blue-800 font-semibold"
                        >
                          Edit
                        </button>
                        <button 
                          onClick={()=>openDeleteModal(u)} 
                          className="text-red-600 hover:text-red-800 font-semibold disabled:text-gray-400 disabled:cursor-not-allowed"
                          disabled={isAdminUser(u)}
                          title={isAdminUser(u) ? "Fixed admin user cannot be deleted" : ""}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-white/70 bg-white/80 p-4 shadow-[0_20px_50px_-35px_rgba(15,23,42,0.45)]">
            <span className="text-sm text-slate-700">
              Showing <strong>{((currentPage-1)*ITEMS_PER_PAGE)+1}</strong> to <strong>{Math.min(currentPage*ITEMS_PER_PAGE, sortedAndFilteredUsers.length)}</strong> of <strong>{sortedAndFilteredUsers.length}</strong> users
            </span>
            <div className="flex gap-2">
              <button 
                onClick={()=>setCurrentPage(p=>Math.max(1,p-1))} 
                disabled={currentPage===1} 
                className="rounded-xl bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>
              <span className="rounded-xl bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700">
                Page {currentPage} of {totalPages}
              </span>
              <button 
                onClick={()=>setCurrentPage(p=>Math.min(totalPages,p+1))} 
                disabled={currentPage===totalPages} 
                className="rounded-xl bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}

          </div>
        </div>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <h3 className="text-2xl font-bold mb-6 text-gray-800">
              {editingUser?'Edit User':'Add New User'}
            </h3>
            
            {formError && (
              <div className="mb-4 p-3 bg-red-50 border-l-4 border-red-500 rounded">
                <div className="flex items-start">
                  <svg className="w-5 h-5 text-red-500 mr-2 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                  <span className="text-red-800 text-sm">{formError}</span>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">Name *</label>
                <input 
                  type="text" 
                  placeholder="Enter full name" 
                  value={formData.name} 
                  onChange={e=>setFormData({...formData,name:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" 
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">College Name</label>
                <input 
                  type="text" 
                  placeholder="Enter college name" 
                  value={formData.collegeName} 
                  onChange={e=>setFormData({...formData,collegeName:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">Email *</label>
                <input 
                  type="email" 
                  placeholder="Enter email address" 
                  value={formData.email} 
                  onChange={e=>{
                    setFormData({...formData,email:e.target.value});
                    setFormError(''); 
                  }} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" 
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                  Password {editingUser && "(leave blank to keep current)"}
                </label>
                <input 
                  type="text" 
                  placeholder={editingUser?"New password (optional)":"Enter password"} 
                  value={formData.password} 
                  onChange={e=>setFormData({...formData,password:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">Status *</label>
                <select 
                  value={formData.status} 
                  onChange={e=>setFormData({...formData,status:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="Active">Active</option>
                  <option value="Blocked">Blocked</option>
                  <option value="Completed Exam">Completed Exam</option>
                </select>
              </div>
              
              <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
                <button 
                  type="button" 
                  onClick={closeModal} 
                  className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300 transition font-semibold"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="bg-blue-600 text-white py-2 px-6 rounded-lg hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition font-semibold"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Saving...' : (editingUser?'Save Changes':'Add User')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isDeleteModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mx-auto mb-4">
              <svg className="w-6 h-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h3 className="text-2xl font-bold mb-2 text-center text-gray-800">Delete User</h3>
            <p className="mb-6 text-center text-gray-600">
              Are you sure you want to delete <span className="font-semibold text-gray-800">{userToDelete?.name}</span>? This action cannot be undone.
            </p>
            <div className="flex justify-center gap-3">
              <button 
                onClick={closeDeleteModal} 
                className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300 transition font-semibold"
              >
                Cancel
              </button>
              <button 
                onClick={handleDelete} 
                className="bg-red-600 text-white py-2 px-6 rounded-lg hover:bg-red-700 transition font-semibold"
              >
                Delete User
              </button>
            </div>
          </div>
        </div>
      )}

      {isAssignExamModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
            <h3 className="text-2xl font-bold mb-4 text-gray-800">Assign Exam</h3>
            <p className="mb-4 text-gray-600">
              Assigning exam to <span className="font-semibold text-blue-600">{selectedUsers.length}</span> selected user(s)
            </p>
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Select Exam</label>
              <select 
                value={selectedExamId} 
                onChange={e => setSelectedExamId(e.target.value)} 
                className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">-- Choose an exam --</option>
                {(Array.isArray(exams) ? exams : []).map(ex => <option key={ex.id} value={ex.id}>{ex.title}</option>)}
              </select>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button 
                onClick={closeAssignExamModal} 
                className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300 transition font-semibold"
              >
                Cancel
              </button>
              <button 
                onClick={() => {
                  if (!selectedExamId) {
                    setNotification({ message: "Please select an exam first", type: "error", persistent: false });
                    return;
                  }
                  handleAssignExam(selectedExamId);
                }} 
                className="bg-green-600 text-white py-2 px-6 rounded-lg hover:bg-green-700 transition font-semibold disabled:bg-green-400 disabled:cursor-not-allowed"
                disabled={!selectedExamId}
              >
                Assign Exam
              </button>
            </div>
          </div>
        </div>
      )}

      {isSlotModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
            <h3 className="text-2xl font-bold mb-6 text-gray-800">
              {editingSlot ? 'Edit Slot' : 'Create New Slot'}
            </h3>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Slot Number *</label>
                <input 
                  type="number"
                  placeholder="Enter slot number (e.g., 1, 2, 3...)"
                  value={slotFormData.slotNumber} 
                  onChange={e=>setSlotFormData({...slotFormData, slotNumber:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                  min="0"
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  College Name
                </label>
                <input 
                  type="text"
                  placeholder="Enter college name (optional)"
                  value={slotFormData.collegeName} 
                  onChange={e=>setSlotFormData({...slotFormData, collegeName:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Slot Password *
                </label>
                <input
                  type="password"
                  placeholder="Enter slot password"
                  value={slotFormData.slotPassword}
                  onChange={e=>setSlotFormData({...slotFormData, slotPassword:e.target.value})}
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  autoComplete="new-password"
                  required
                />
                <p className="text-xs text-gray-500 mt-1">
                  This password will be assigned to users created from Excel upload for this slot.
                </p>
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Pass Percentage *
                </label>
                <input
                  type="number"
                  placeholder="Enter pass percentage (0-100)"
                  value={slotFormData.passPercentage}
                  onChange={e=>setSlotFormData({...slotFormData, passPercentage:e.target.value})}
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  min="0"
                  max="100"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Select Date *
                </label>
                <input 
                  type="date" 
                  value={slotFormData.date} 
                  onChange={e=>setSlotFormData({...slotFormData, date:e.target.value})} 
                  className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
              
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Select Time *
                </label>
                <div className="grid grid-cols-3 gap-3">
                  <select
                    value={slotTimeInput.hour}
                    onChange={e => handleSlotTimeChange('hour', e.target.value)}
                    className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Hour</option>
                    {SLOT_HOURS.map(hour => (
                      <option key={hour} value={hour}>{hour}</option>
                    ))}
                  </select>
                  <select
                    value={slotTimeInput.minute}
                    onChange={e => handleSlotTimeChange('minute', e.target.value)}
                    className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="">Minute</option>
                    {SLOT_MINUTES.map(minute => (
                      <option key={minute} value={minute}>{minute}</option>
                    ))}
                  </select>
                  <select
                    value={slotTimeInput.period}
                    onChange={e => handleSlotTimeChange('period', e.target.value)}
                    className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="AM">AM</option>
                    <option value="PM">PM</option>
                  </select>
                </div>
                <p className="text-xs text-gray-500 mt-1">
                  Time is selected in 12-hour format and saved automatically for the backend.
                </p>
              </div>
            </div>
            
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button 
                onClick={()=>setIsSlotModalOpen(false)} 
                className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300 transition font-semibold"
                disabled={isSlotSubmitting}
              >
                Cancel
              </button>
              <button 
                onClick={handleCreateOrUpdateSlot} 
                className="bg-blue-600 text-white py-2 px-6 rounded-lg hover:bg-blue-700 transition font-semibold disabled:bg-blue-400 disabled:cursor-not-allowed"
                disabled={isSlotSubmitting}
              >
                {isSlotSubmitting ? (editingSlot ? 'Updating...' : 'Creating...') : (editingSlot ? 'Update Slot' : 'Create Slot')}
              </button>
            </div>
          </div>
        </div>
      )}

      {isDeleteSlotModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mx-auto mb-4">
              <svg className="w-6 h-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h3 className="text-2xl font-bold mb-2 text-center text-gray-800">Delete Slot</h3>
            <p className="mb-6 text-center text-gray-600">
              Are you sure you want to delete <span className="font-semibold text-gray-800">Slot {slotToDelete?.slotNumber}</span>? 
              {slotToDelete && allUsers.filter(u => u.slotNumber === slotToDelete.slotNumber).length > 0 && (
                <span className="block mt-2 text-red-600 font-semibold">
                  Warning: This slot has {allUsers.filter(u => u.slotNumber === slotToDelete.slotNumber).length} user(s). 
                  They will be removed from this slot.
                </span>
              )}
            </p>
            <div className="flex justify-center gap-3">
              <button 
                onClick={closeDeleteSlotModal} 
                className="bg-gray-200 py-2 px-6 rounded-lg hover:bg-gray-300 transition font-semibold"
              >
                Cancel
              </button>
              <button 
                onClick={handleDeleteSlot} 
                className="bg-red-600 text-white py-2 px-6 rounded-lg hover:bg-red-700 transition font-semibold"
              >
                Delete Slot
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
