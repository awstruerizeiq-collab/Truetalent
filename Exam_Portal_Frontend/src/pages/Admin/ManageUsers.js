import React, { useState, useEffect, useMemo, useCallback } from "react";
import axios from "../../api/axiosConfig";


const TOTAL_SLOTS = 10;
const SLOT_NAMES = Array.from({ length: TOTAL_SLOTS }, (_, i) => `Slot ${i + 1}`);


const Notification = ({ message, type, onClose }) => {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [message, onClose]);

  if (!message) return null;

  return (
    <div className={`fixed top-5 right-5 p-4 rounded-lg shadow-lg text-white z-50 ${type==='success'?'bg-green-500':'bg-red-500'}`}>
      {message} <button onClick={onClose} className="ml-4 font-bold">✕</button>
    </div>
  );
};


const StatusBadge = ({ status }) => {
  const statusStyles = {
    Active: 'bg-green-100 text-green-800',
    Blocked: 'bg-red-100 text-red-800',
    'Completed Exam': 'bg-blue-100 text-blue-800',
    'Assigned Exam': 'bg-yellow-100 text-yellow-800',
  };
  return <span className={`px-2 py-1 rounded-full text-xs font-semibold ${statusStyles[status]}`}>{status}</span>;
};


export default function ManageUsers() {
  const ITEMS_PER_PAGE = 5;

  
  const [allUsers, setAllUsers] = useState([]);
  const [currentSlot, setCurrentSlot] = useState(SLOT_NAMES[0]); 
  const [exams, setExams] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isAssignExamModalOpen, setIsAssignExamModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [userToDelete, setUserToDelete] = useState(null);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [selectedExamId, setSelectedExamId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [sortConfig, setSortConfig] = useState({ key: 'id', direction: 'ascending' });
  const [notification, setNotification] = useState({ message: '', type: '' });
  const [currentPage, setCurrentPage] = useState(1);
  const [formData, setFormData] = useState({ name: '', email: '', collegeName: '', password: '', status: 'Active' });
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  
  const fetchUsers = useCallback(async () => {
    try {
      const res = await axios.get('/admin/users', { withCredentials: true });
      setAllUsers(res.data);
      setSelectedUsers([]);
      setCurrentPage(1);
    } catch (err) { 
      console.error(err);
      setNotification({ message: 'Failed to fetch users', type: 'error' });
    }
  }, []);

  const fetchExams = async () => {
    try {
      const res = await axios.get('/admin/exams', { withCredentials: true });
      setExams(res.data);
    } catch (err) { 
      console.error(err);
      setNotification({ message: 'Failed to fetch exams', type: 'error' });
    }
  };

  useEffect(() => { fetchUsers(); fetchExams(); }, [fetchUsers]);

  
  useEffect(() => {
    window.history.pushState(null, "", window.location.href);
    
    const handlePopState = (event) => {
      window.history.pushState(null, "", window.location.href);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

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
  const closeAssignExamModal = () => setIsAssignExamModalOpen(false);

  
  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setIsSubmitting(true);

    try {
      if (editingUser) {
        await axios.put(`/admin/users/${editingUser.id}`, formData, { withCredentials: true });
        setNotification({ message: 'User updated successfully!', type:'success' });
      } else {
        
        const payload = {
          ...formData,
          slotNumber: SLOT_NAMES.indexOf(currentSlot) + 1
        };
        await axios.post(`/auth/addCandidate`, payload, { withCredentials: true });
        setNotification({ message: 'User added successfully!', type:'success' });
      }
      fetchUsers();
      closeModal();
    } catch(err){
      console.error('Error saving user:', err);
      
      
      if (err.response) {
        const errorMessage = err.response.data?.message || err.response.data?.error;
        
        if (err.response.status === 400 || err.response.status === 409) {
          
          if (errorMessage && errorMessage.includes('already exists')) {
            setFormError('This email is already registered. Please use a different email.');
          } else if (errorMessage) {
            setFormError(errorMessage);
          } else {
            setFormError('Failed to save user. Please check your input.');
          }
        } else if (err.response.status === 500) {
          setFormError(' This email is already registered. Please use a different email');
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
      await axios.delete(`/admin/users/${userToDelete.id}`, { withCredentials: true });
      setNotification({ message: `User ${userToDelete.name} deleted.`, type:'success' });
      fetchUsers();
      closeDeleteModal();
    } catch(err){ 
      console.error(err);
      setNotification({ message: 'Failed to delete user', type: 'error' });
    }
  };

  const handleAssignExam = async (examId) => {
    if (!examId || selectedUsers.length === 0) return;
    try {
      const payload = {
        examId: Number(examId),
        userIds: selectedUsers.map(id => Number(id))
      };
      await axios.post(`/admin/users/assign-exam`, payload, { withCredentials: true });
      setNotification({ message: `Exam assigned to ${selectedUsers.length} user(s).`, type:'success' });
      setSelectedUsers([]);
      fetchUsers();
      closeAssignExamModal();
    } catch(err){
      console.error("Assign Exam Error:", err);
      const errorMsg = err.response?.data?.message || 'Failed to assign exam';
      setNotification({ message: errorMsg, type:'error' });
    }
  };

  const handleBulkDelete = async () => {
    if (!window.confirm(`Are you sure you want to delete ${selectedUsers.length} user(s)?`)) {
      return;
    }
    
    try {
      await Promise.all(selectedUsers.map(id => axios.delete(`/admin/users/${id}`, { withCredentials: true })));
      setNotification({ message: `${selectedUsers.length} user(s) deleted.`, type:'success' });
      setSelectedUsers([]);
      fetchUsers();
    } catch(err){ 
      console.error(err);
      setNotification({ message: 'Failed to delete some users', type: 'error' });
    }
  };

  
  const requestSort = (key) => {
    let direction='ascending';
    if(sortConfig.key===key && sortConfig.direction==='ascending') direction='descending';
    setSortConfig({ key, direction });
  };

  
  const sortedAndFilteredUsers = useMemo(() => {
    const slotIndex = SLOT_NAMES.indexOf(currentSlot) + 1;
    const usersInSlot = allUsers.filter(u => u.slotNumber === slotIndex);

    let filtered = usersInSlot.filter(u =>
      (u.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
       u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
       (u.collegeName && u.collegeName.toLowerCase().includes(searchTerm.toLowerCase()))) &&
      (statusFilter==='All' || u.status===statusFilter)
    );

    filtered.sort((a,b)=>{
      if(a[sortConfig.key]<b[sortConfig.key]) return sortConfig.direction==='ascending'?-1:1;
      if(a[sortConfig.key]>b[sortConfig.key]) return sortConfig.direction==='ascending'?1:-1;
      return 0;
    });
    return filtered;
  }, [allUsers, currentSlot, searchTerm, statusFilter, sortConfig]);

  // --- Pagination ---
  const totalPages = Math.ceil(sortedAndFilteredUsers.length / ITEMS_PER_PAGE);
  const paginatedUsers = useMemo(() => {
    const start=(currentPage-1)*ITEMS_PER_PAGE;
    return sortedAndFilteredUsers.slice(start, start+ITEMS_PER_PAGE);
  }, [currentPage, sortedAndFilteredUsers]);

  const handleSelectUser = (id) => setSelectedUsers(prev => prev.includes(id)?prev.filter(x=>x!==id):[...prev,id]);
  const handleSelectAllOnPage = (e) => {
    const pageIds = paginatedUsers.map(u=>u.id);
    if(e.target.checked) setSelectedUsers(prev=>[...new Set([...prev, ...pageIds])]);
    else setSelectedUsers(prev=>prev.filter(id=>!pageIds.includes(id)));
  };
  const areAllOnPageSelected = paginatedUsers.length>0 && paginatedUsers.every(u=>selectedUsers.includes(u.id));

  const handleSlotChange = (slot) => {
    setCurrentSlot(slot);
    setCurrentPage(1);
    setSelectedUsers([]);
    setSearchTerm('');
    setStatusFilter('All');
  };

  useEffect(()=>{ if(currentPage>totalPages && totalPages>0) setCurrentPage(totalPages); },[currentPage,totalPages]);

  
  return (
    <div className="p-8 bg-gray-50 min-h-screen">
      <Notification message={notification.message} type={notification.type} onClose={()=>setNotification({message:'',type:''})}/>
      <h2 className="text-4xl font-bold mb-6 text-gray-800">Manage Users (Slot: {currentSlot})</h2>

      
      <div className="bg-white p-4 rounded-lg shadow-md mb-6 overflow-x-auto whitespace-nowrap">
        {SLOT_NAMES.map(slot => {
          const slotIndex = SLOT_NAMES.indexOf(slot) + 1;
          const count = allUsers.filter(u => u.slotNumber === slotIndex).length;
          return (
            <button
              key={slot}
              onClick={() => handleSlotChange(slot)}
              className={`px-4 py-2 m-1 rounded-lg font-semibold transition-colors duration-200 ${
                currentSlot === slot
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'bg-gray-200 text-gray-700 hover:bg-blue-100'
              }`}
            >
              {slot} ({count})
            </button>
          );
        })}
      </div>

      <div className="bg-white p-4 rounded-lg shadow-md mb-6 flex justify-between items-center">
        <input type="text" placeholder="Search..." value={searchTerm} onChange={e=>setSearchTerm(e.target.value)} className="px-4 py-2 border rounded w-1/3"/>
        <select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)} className="px-4 py-2 border rounded">
          <option value="All">All Statuses</option>
          <option value="Active">Active</option>
          <option value="Blocked">Blocked</option>
          <option value="Completed Exam">Completed Exam</option>
          <option value="Assigned Exam">Assigned Exam</option>
        </select>
        <div className="flex space-x-2">
          {selectedUsers.length>0 && <>
            <button onClick={handleBulkDelete} className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600">Delete Selected ({selectedUsers.length})</button>
            <button onClick={openAssignExamModal} className="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600">Assign Exam ({selectedUsers.length})</button>
          </>}
          <button onClick={()=>openModal()} className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">Add User</button>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-xl overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-200 text-gray-700 uppercase text-sm">
            <tr>
              <th className="py-3 px-4 text-left">
                <input type="checkbox" onChange={handleSelectAllOnPage} checked={areAllOnPageSelected}/>
              </th>
              <th className="py-3 px-4 text-left">Sl No.</th>
              <th className="py-3 px-4 text-left cursor-pointer" onClick={()=>requestSort('id')}>ID</th>
              <th className="py-3 px-6 text-left cursor-pointer" onClick={()=>requestSort('name')}>Name</th>
              <th className="py-3 px-6 text-left cursor-pointer" onClick={()=>requestSort('collegeName')}>College</th>
              <th className="py-3 px-6 text-left cursor-pointer" onClick={()=>requestSort('email')}>Email</th>
              <th className="py-3 px-6 text-left">Password</th>
              <th className="py-3 px-6 text-left cursor-pointer" onClick={()=>requestSort('status')}>Status</th>
              <th className="py-3 px-6 text-left">Assigned Exams</th>
              <th className="py-3 px-6 text-center">Actions</th>
            </tr>
          </thead>
          <tbody className="text-gray-700 text-sm">
            {paginatedUsers.map(u => (
              <tr key={u.id} className="border-b hover:bg-gray-100">
                <td className="py-3 px-4">
                  <input type="checkbox" checked={selectedUsers.includes(u.id)} onChange={()=>handleSelectUser(u.id)}/>
                </td>
                <td className="py-3 px-4">
                  {((currentPage - 1) * ITEMS_PER_PAGE) + paginatedUsers.indexOf(u) + 1}
                </td>
                <td className="py-3 px-4">{u.id}</td>
                <td className="py-3 px-6">{u.name}</td>
                <td className="py-3 px-6">{u.collegeName || 'N/A'}</td>
                <td className="py-3 px-6">{u.email}</td>
                <td className="py-3 px-6 font-mono">{u.password}</td>
                <td className="py-3 px-6"><StatusBadge status={u.status}/></td>
                <td className="py-3 px-6">
                  {u.assignedExams?.length > 0
                    ? u.assignedExams.map(ex => ex.title).join(', ')
                    : 'None'}
                </td>
                <td className="py-3 px-6 text-center flex space-x-4 justify-center">
                  <button onClick={()=>openModal(u)} className="text-blue-600 hover:text-blue-800">Edit</button>
                  <button onClick={()=>openDeleteModal(u)} className="text-red-600 hover:text-red-800">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      
      <div className="flex justify-between items-center mt-4">
        <span className="text-sm text-gray-700">Page <strong>{currentPage}</strong> of <strong>{totalPages}</strong></span>
        <div className="flex space-x-2">
          <button onClick={()=>setCurrentPage(p=>Math.max(1,p-1))} disabled={currentPage===1} className="px-3 py-1 bg-gray-200 rounded disabled:opacity-50">Previous</button>
          <button onClick={()=>setCurrentPage(p=>Math.min(totalPages,p+1))} disabled={currentPage===totalPages} className="px-3 py-1 bg-gray-200 rounded disabled:opacity-50">Next</button>
        </div>
      </div>

      
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-lg">
            <h3 className="text-2xl font-bold mb-6">{editingUser?'Edit User':'Add User'}</h3>
            
            {formError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                <div className="flex items-start">
                  <svg className="w-5 h-5 text-red-500 mr-2 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                  <span className="text-red-800 text-sm">{formError}</span>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <input 
                type="text" 
                placeholder="Name" 
                value={formData.name} 
                onChange={e=>setFormData({...formData,name:e.target.value})} 
                className="w-full p-2 border rounded" 
                required
              />
              <input 
                type="text" 
                placeholder="College" 
                value={formData.collegeName} 
                onChange={e=>setFormData({...formData,collegeName:e.target.value})} 
                className="w-full p-2 border rounded"
              />
              <input 
                type="email" 
                placeholder="Email" 
                value={formData.email} 
                onChange={e=>{
                  setFormData({...formData,email:e.target.value});
                  setFormError(''); 
                }} 
                className="w-full p-2 border rounded" 
                required
              />
              <input 
                type="text" 
                placeholder={editingUser?"New Password (optional)":"Password"} 
                value={formData.password} 
                onChange={e=>setFormData({...formData,password:e.target.value})} 
                className="w-full p-2 border rounded"
              />
              <select 
                value={formData.status} 
                onChange={e=>setFormData({...formData,status:e.target.value})} 
                className="w-full p-2 border rounded"
              >
                <option value="Active">Active</option>
                <option value="Blocked">Blocked</option>
                <option value="Completed Exam">Completed Exam</option>
              </select>
              <div className="flex justify-end space-x-3 mt-6">
                <button 
                  type="button" 
                  onClick={closeModal} 
                  className="bg-gray-300 py-2 px-5 rounded-lg hover:bg-gray-400"
                  disabled={isSubmitting}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="bg-blue-600 text-white py-2 px-5 rounded-lg hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed"
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
            <h3 className="text-2xl font-bold mb-4">Delete User</h3>
            <p className="mb-6">Are you sure you want to delete <span className="font-semibold">{userToDelete?.name}</span>?</p>
            <div className="flex justify-end space-x-3">
              <button onClick={closeDeleteModal} className="bg-gray-300 py-2 px-5 rounded-lg hover:bg-gray-400">Cancel</button>
              <button onClick={handleDelete} className="bg-red-600 text-white py-2 px-5 rounded-lg hover:bg-red-700">Delete</button>
            </div>
          </div>
        </div>
      )}

      
      {isAssignExamModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white p-8 rounded-xl shadow-2xl w-full max-w-md">
            <h3 className="text-2xl font-bold mb-4">Assign Exam</h3>
            <p className="mb-4">Assigning to {selectedUsers.length} user(s).</p>
            <select value={selectedExamId} onChange={e => setSelectedExamId(e.target.value)} className="w-full p-2 border rounded">
              <option value="">-- Choose an exam --</option>
              {exams.map(ex => <option key={ex.id} value={ex.id}>{ex.title}</option>)}
            </select>
            <div className="flex justify-end mt-6 space-x-3">
              <button onClick={closeAssignExamModal} className="bg-gray-300 py-2 px-5 rounded-lg hover:bg-gray-400">Cancel</button>
              <button onClick={() => {
                if (!selectedExamId) {
                  setNotification({ message: "Please select an exam first", type: "error" });
                  return;
                }
                handleAssignExam(selectedExamId);
              }} className="bg-green-600 text-white py-2 px-5 rounded-lg hover:bg-green-700">Assign Exam</button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
