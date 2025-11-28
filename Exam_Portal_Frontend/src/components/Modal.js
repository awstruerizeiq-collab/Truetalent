import React from 'react';

function Modal({ isOpen, title, children, onClose, onSubmit }) {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h2>{title}</h2>
        <div className="modal-content">{children}</div>
        <div className="modal-actions">
          <button className="btn cancel" onClick={onClose}>Cancel</button>
          <button className="btn submit" onClick={onSubmit}>Submit</button>
        </div>
      </div>
    </div>
  );
}

export default Modal;
