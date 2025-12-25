import React from 'react';
import Modal from './Modal';

const NotificationModal = ({ isOpen, title, message, onClose }) => {
  return (
    <Modal
      isOpen={isOpen}
      title={title}
      onClose={onClose}
      cancelText="OK"
    >
      <p className="text-gray-700 whitespace-pre-line">{message}</p>
    </Modal>
  );
};

export default NotificationModal;
