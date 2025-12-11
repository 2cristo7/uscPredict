import { Link } from 'react-router-dom';

const Footer = () => {
  return (
    <footer className="bg-[#0a0a0a] border-t border-[#1a1a1a] mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Logo */}
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-[#3b82f6] rounded-lg flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
            <span className="text-base font-semibold text-white">USCPredict</span>
          </div>

          {/* Links */}
          <nav className="flex items-center gap-8">
            <Link
              to="/"
              className="text-sm text-[#888888] hover:text-white transition-colors"
            >
              Markets
            </Link>
            <Link
              to="/profile"
              className="text-sm text-[#888888] hover:text-white transition-colors"
            >
              Portfolio
            </Link>
          </nav>

          {/* Copyright */}
          <p className="text-sm text-[#666666]">
            {new Date().getFullYear()} USCPredict. Educational project.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
