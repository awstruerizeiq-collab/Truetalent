import React from "react";
import Navbar from "./Navbar"; 
import Sidebar from "./Sidebar"; 

const PageLayout = ({ children }) => {
  return (
    
    <div className="flex h-screen bg-gray-100">
      
      {}
      <Sidebar />

      {}
      <div className="flex-1 flex flex-col overflow-hidden">
        
        {}
        <Navbar />

        {}
        <main className="flex-1 overflow-y-auto">
          <div className="page-content p-8"> {}
            {children}
          </div>
        </main>

      </div>
    </div>
  );
};

export default PageLayout;