import React from 'react';
import truerizeLogo from '../assets/images/Truerize_Logo.png';

function Navbar() {
  return (
    <nav className="flex items-center justify-between border-b border-white/60 bg-white/80 px-6 py-4 shadow-[0_12px_30px_-20px_rgba(15,23,42,0.35)] backdrop-blur-sm">
      <div className="flex items-center gap-4">
        <img
          src="https://media.licdn.com/dms/image/v2/D4D0BAQFNTy-9Xe_LkA/company-logo_200_200/B4DZidpNLlHwAI-/0/1754991484679/truerizeiq_strategic_solutions_pvt_ltd_logo?e=2147483647&v=beta&t=r-cD09Gb31nOFALTvUFJicsVAONy17v-8o0GL1OKW9U"
          alt="Truerize Logo"
          className="h-14 w-14 rounded-xl object-contain shadow-sm"
        />
        <div className="text-xl sm:text-2xl font-semibold text-slate-900">
          Truerize Talent Portal
        </div>
      </div>
      <div>
        <span className="inline-flex items-center rounded-full border border-blue-200/70 bg-blue-50/70 px-4 py-1 text-sm font-semibold text-blue-700">
          Admin Dashboard
        </span>
      </div>
    </nav>
  );
}

export default Navbar;
