import React from 'react';

function Modal({ isOpen, title, children, onClose, onSubmit, submitText = 'Submit', cancelText = 'Cancel' }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">{title}</h2>
        <div className="modal-content mb-6">{children}</div>
        <div className="flex justify-end space-x-3">
          <button
            className="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors"
            onClick={onClose}
          >
            {cancelText}
          </button>
          {onSubmit && (
            <button
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              onClick={onSubmit}
            >
              {submitText}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default Modal;
