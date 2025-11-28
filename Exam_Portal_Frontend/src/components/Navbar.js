import React from 'react';
import truerizeLogo from '../assets/images/Truerize_Logo.png';

function Navbar() {
  return (
    <nav
      style={{
        background: 'white', 
        color: '#000080', 
        padding: '1rem',
        boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
        borderBottomLeftRadius: '1rem',
        borderBottomRightRadius: '1rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <img
          src="https://media.licdn.com/dms/image/v2/D4D0BAQFNTy-9Xe_LkA/company-logo_200_200/B4DZidpNLlHwAI-/0/1754991484679/truerizeiq_strategic_solutions_pvt_ltd_logo?e=2147483647&v=beta&t=r-cD09Gb31nOFALTvUFJicsVAONy17v-8o0GL1OKW9U"
          alt="Truerize Logo"
          style={{
            height: '80px',
            width: '80px',
            objectFit: 'contain',
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)', 
          }}
        />
      <div
  style={{
    fontSize: '1.875rem', 
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif', 
    letterSpacing: 'normal', 
    color: '#1e3a8a', 
    textShadow: 'none', 
  }}
>
  Truerize Exam Portal
</div>
      </div>
    <div>
  <span style={{ 
    fontSize: '1.25rem', 
    fontWeight: '600', 
    color: '#333',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif'
  }}>
    Admin Dashboard
  </span>
</div>
    </nav>
  );
}

export default Navbar;
